//
// OrcEngine.java -- Java class OrcEngine
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.runtime;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import orc.Config;
import orc.error.SourceLocation;
import orc.error.runtime.TokenException;
import orc.error.runtime.TokenLimitReachedError;
import orc.runtime.nodes.Node;
import orc.runtime.regions.Execution;
import orc.runtime.regions.LogicalClock;
import orc.runtime.values.Value;
import orc.trace.Tracer;

/**
 * The Orc Engine provides the main loop for executing active tokens. Tokens are
 * always processed in a single thread, but tokens might be activated or resumed
 * from other threads, so some synchronization is necessary.
 * 
 * @author wcook, dkitchin, quark
 */
public class OrcEngine implements Runnable {
	/** Tokens which are ready to be processed. */
	private final LinkedList<Token> readyTokens = new LinkedList<Token>();

	/**
	 * Tokens which have returned from a site. These are distinguished from
	 * {@link #readyTokens} to ensure that all ready tokens are processed before
	 * pending site returns.
	 */
	private final LinkedList<Token> queuedReturns = new LinkedList<Token>();

	private int round = 1;

	private final int debugLevel;

	/**
	 * This flag is set by the root {@link LogicalClock} when execution completes to
	 * terminate the engine.
	 */
	private boolean halt = false;

	/**
	 * This flag is set externally to pause execution.
	 */
	private boolean pause = false;

	/**
	 * Currently this reference is just needed to keep pending tokens from being
	 * garbage-collected prematurely.
	 */
	private Execution region;

	/**
	 * Scheduler thread for Rtimer. This is here instead of the Rtimer site so
	 * we can easily cancel it when the job completes.
	 * 
	 * @see #scheduleTimer(TimerTask, long)
	 */
	private Timer timer;

	/** Factory for tokens. */
	final TokenPool pool;

	private final Config config;
	private final int maxpubs;
	private int pubs = 0;
	private final PrintStream stdout;
	private final PrintStream stderr;

	/** We'll notify this when the computation is finished. */
	private Tracer tracer;

	public final static Globals<OrcEngine, Object> globals = new Globals<OrcEngine, Object>();

	public OrcEngine(final Config config) {
		this.config = config;
		this.maxpubs = config.getMaxPubs();
		this.debugLevel = config.getDebugLevel();
		this.pool = config.getTokenPool();
		this.stdout = config.getStdout();
		this.stderr = config.getStderr();
	}

	public final synchronized boolean isDead() {
		return halt;
	}

	/**
	 * Process active nodes, running indefinitely until signalled to stop by a
	 * call to terminate(). Typically you will use one of the other run methods
	 * to queue an active token to process first.
	 */
	public final void run() {
		debug(1, "Started engine.");
		timer = new Timer();
		Kilim.startEngine(config.getNumKilimThreads(), config.getNumSiteThreads());
		while (true) {
			if (!step()) {
				break;
			}
		}
		timer.cancel();
		timer = null;
		Kilim.stopEngine();
		tracer.finish();
		tracer = null;
		globals.removeAll(this);
		synchronized (this) {
			for (final File tmpdir : tmpdirs) {
				deleteDirectory(tmpdir);
			}
			tmpdirs.clear();
		}
		onTerminate();
	}

	/** Override this to customize termination. */
	public void onTerminate() {
		// do nothing
	}

	/**
	 * Terminate execution.
	 */
	public final synchronized void terminate() {
		halt = true;
		debug(1, "Engine terminated.");
		notifyAll();
	}

	/**
	 * Pause execution.
	 */
	public final synchronized void pause() {
		pause = true;
		debug(1, "Engine paused.");
	}

	/**
	 * Unpause execution.
	 */
	public final synchronized void unpause() {
		pause = false;
		debug(1, "Engine unpaused.");
		// FIXME: do we need notifyAll or just notify?
		notifyAll();
	}

	/**
	 * Run Orc given a root node. Creates an initial environment and then
	 * executes the main loop.
	 * 
	 * @param root
	 *            node to run
	 */
	public final void run(final Node root) {
		start(root);
		run();
	}

	public final void start(final Node root) {
		assert root != null;
		region = new Execution(this);
		tracer = config.getTracer();
		Token token;
		try {
			token = pool.newToken();
		} catch (final TokenLimitReachedError e) {
			// This should be impossible
			throw new AssertionError(e);
		}
		token.initializeRoot(root, region, this, tracer.start());
		activate(token);
	}

	/**
	 * Run one step (process one token or handle one site response) Return false
	 * if engine should halt.
	 */
	private final boolean step() {
		// the next token to process
		Token todo;

		// synchronize while we decide what to do next;
		// this block will either return from the function
		// or set todo to a token to process
		synchronized (this) {
			if (halt) {
				return false;
			} else if (pause) {
				try {
					wait();
				} catch (final InterruptedException e) {
					// return to the main loop and check for termination
				}
				return true;
			} else if (!readyTokens.isEmpty()) {
				// If an active token is available, process it.
				todo = readyTokens.remove();
			} else if (!queuedReturns.isEmpty()) {
				// If a site return is available, make it active.
				// This marks the beginning of a new round.
				readyTokens.add(queuedReturns.remove());
				round++;
				reportRound();
				return true;
			} else {
				try {
					// we will be notified when a site returns
					// or the engine terminates
					wait();
				} catch (final InterruptedException e) {
					// return to the main loop and check for termination
				}
				return true;
			}
		}
		// notice that we do not synchronize while
		// processing a token; we don't want to risk
		// locking the engine if the processing blocks
		if (shouldDebug(3)) {
			debug(3, "Processing " + todo + ":\n" + "  " + todo.getSourceLocation() + "\n" + "  Node: " + todo.getNode());
		}
		todo.process();
		return true;
	}

	/**
	 * Activate a token by adding it to the queue of active tokens
	 * 
	 * @param t
	 *            the token to be added
	 */
	public final synchronized void activate(final Token t) {
		readyTokens.addLast(t);
		notifyAll();
	}

	/**
	 * Activate a token by adding it to the queue of returning tokens
	 * 
	 * @param t
	 *            the token to be added
	 */
	public final synchronized void resume(final Token t) {
		t.getTracer().receive(t.getResult());
		queuedReturns.addLast(t);
		notifyAll();
	}

	/**
	 * Publish a result. This method is called by the Pub node when a
	 * publication is 'escaping' the bottom of the execution graph.
	 * 
	 * @param v
	 */
	public final void publish(final Object v) {
		onPublish(v);
		// check if we have reached the maximum allowed number of publications
		if (maxpubs > 0 && maxpubs == ++pubs) {
			terminate();
		}
	}

	/**
	 * Handle a published value.
	 * The default implementation prints the value's string representation to
	 * the console. Change this behavior by extending OrcEngine and overriding
	 * this method.
	 */
	public void onPublish(final Object v) {
		stdout.println(Value.write(v));
		stdout.flush();
	}

	/**
	 * A token owned by this engine has encountered an exception. The token
	 * dies, remaining silent and leaving the execution, and then calls this
	 * method so that the engine can report or otherwise handle the failure.
	 */
	public final void tokenError(final TokenException problem) {
		onError(problem);
	}

	/** Handle an error token. */
	public void onError(final TokenException problem) {
		stderr.println("Error: " + problem.getMessage());

		if (config.getShortErrors()) {
			return;
		}

		stderr.println("Backtrace:");
		final SourceLocation[] backtrace = problem.getBacktrace();

		final int len = backtrace.length;
		if (len > 0) {
			stderr.println(backtrace[0]);
			final String caret = backtrace[0].getCaret();
			if (caret != null) {
				stderr.println(caret);
			}
		}
		for (int i = 1; i < len; i++) {
			stderr.println(backtrace[i]);
		}
		stderr.println("");
		// HACK: technically, we should report this information
		// using debug(...), but then we couldn't use printStackTrace.
		if (shouldDebug(1)) {
			problem.printStackTrace(stderr);
		}
		final Throwable cause = problem.getCause();
		if (shouldDebug(1) && cause != null) {
			stderr.println("Caused by:");
			cause.printStackTrace(stderr);
		}
	}

	/**
	 * Return true if a debug report at the given level
	 * will be used.  Useful if you want to avoid some expensive
	 * computation just to generate debug output which will be
	 * ignored.
	 */
	public final boolean shouldDebug(final int level) {
		return debugLevel >= level;
	}

	/**
	 * Output some debugging information at the given level.
	 * Level 1 is the least verbose (most important information) debugging level,
	 * and higher numbers are used for more verbose (less important information).
	 * @param level
	 * @param s
	 */
	public final void debug(final int level, final String s) {
		assert level > 0;
		if (debugLevel >= level) {
			onDebug(s);
		}
	}

	/**
	 * Override this to change how debugging output is handled.
	 * @param s
	 */
	public void onDebug(final String s) {
		stderr.println(s);
	}

	public final void reportRound() {
		if (shouldDebug(2)) {
			debug(2, "Round:   " + round + "\n" + "Active:  " + readyTokens.size() + "\n" + "Queued:  " + queuedReturns.size() + "\n");
		}
	}

	/**
	 * Print something (for use by the print and println sites). By default,
	 * this prints to stdout, but this can be overridden to do something
	 * else if appropriate.
	 * 
	 * @see Token#print(String, boolean)
	 */
	public void print(final String value, final boolean newline) {
		if (newline) {
			stdout.println(value);
		} else {
			stdout.print(value);
		}
	}

	/**
	 * Schedule a timed task (used by Rtimer).
	 */
	public final void scheduleTimer(final TimerTask task, final long delay) {
		timer.schedule(task, delay);
	}

	public final String addGlobal(final Object value) {
		return globals.add(this, value);
	}

	public final Config getConfig() {
		return config;
	}

	public final Execution getExecution() {
		return region;
	}

	private final static String tmpdir = System.getProperty("java.io.tmpdir");
	private final LinkedList<File> tmpdirs = new LinkedList<File>();

	/**
	 * Create a new temporary directory and return the path to that directory.
	 * The directory will be deleted automatically when the engine halts,
	 * or you can delete it earlier with deleteTmpdir
	 */
	public final synchronized File createTmpdir() throws IOException {
		File out;
		do {
			out = new File(OrcEngine.tmpdir, "orc-" + UUID.randomUUID().toString());
		} while (out.exists());
		if (!out.mkdir()) {
			throw new IOException("Unable to create temporary directory " + out.getPath());
		}
		tmpdirs.add(out);
		return out;
	}

	/** Delete a temporary directory */
	public final synchronized boolean deleteTmpdir(final File directory) {
		tmpdirs.remove(directory);
		return deleteDirectory(directory);
	}

	/** Delete a directory recursively */
	private final boolean deleteDirectory(final File directory) {
		boolean out = true;
		final File[] fileArray = directory.listFiles();
		if (fileArray != null) {
			for (final File f : fileArray) {
				out = deleteDirectory(f) && out;
			}
		}
		return directory.delete() && out;
	}
}
