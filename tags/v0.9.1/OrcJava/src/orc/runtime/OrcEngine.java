/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime;

import java.io.File;
import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import orc.env.Env;
import orc.error.TokenException;
import orc.runtime.nodes.Node;
import orc.runtime.regions.Execution;
import orc.runtime.regions.Region;
import orc.runtime.values.Future;
import orc.runtime.values.GroupCell;
import orc.runtime.values.Value;

/**
 * The Orc Engine provides the main loop for executing active tokens.
 * Tokens are always processed in a single thread, but tokens might
 * be activated or resumed from other threads, so some synchronization
 * is necessary. 
 * 
 * @author wcook, dkitchin, quark
 */
public class OrcEngine implements Runnable {

	LinkedList<Token> activeTokens = new LinkedList<Token>();
	LinkedList<Token> queuedReturns = new LinkedList<Token>();
	Set<LogicalClock> clocks = new HashSet<LogicalClock>();
	int round = 1;
	public boolean debugMode = false;
	/**
	 * This flag is set by the Execution region when execution completes
	 * to terminate the engine.
	 */
	protected boolean halt = false;
	/**
	 * Currently this reference is just needed to keep pending tokens
	 * from being garbage-collected prematurely.
	 */
	private Execution region;
	
	public synchronized boolean isDead() { return halt; }

	/**
	 * Process active nodes, running indefinitely until
	 * signalled to stop by a call to terminate().
	 * Typically you will use one of the other run methods
	 * to queue an active token to process first.
	 */
	public void run() {
		while (true) {
			// FIXME: can we avoid synchronizing this whole block?
			synchronized(this) {
				if (halt) return;
				if (!step()) {
					try {
						wait();
					} catch (InterruptedException e) {
						// do nothing
					}
				}
			}
		}
	}
	
	/**
	 * Terminate execution.
	 */
	public synchronized void terminate() {
		halt = true;
		debug("Engine terminated.");
		notifyAll();
	}

	/**
	 * Run Orc given a root node.
	 * Creates an initial environment and then 
	 * executes the main loop.
	 * @param root  node to run
	 */ 
	public void run(Node root) {
		start(root);
		run();
	}

	public void run(Node root, Env env) {
		start(root, env);
		run();
	}
	
	public void start(Node root) {
		start(root, new Env<Future>());
	}
	
	public void start(Node root, Env env) {
		assert(root != null);
		assert(env != null);
		region = new Execution(this);
		activate(new Token(root, env, null, new GroupCell(), region, null, this));
	}
	
	/**
	 * Run one step (process one token, handle one site response, or advance
	 * all logical clocks). Returns true if work was done.
	 */
	protected boolean step() {
		/* If an active token is available, process it. */
		if (!activeTokens.isEmpty()){
			activeTokens.remove().process();
			return true;
		}
		
		/* If a site return is available, make it active.
		 * This marks the beginning of a new round. 
		 */
		if (!queuedReturns.isEmpty()){
			activeTokens.add(queuedReturns.remove());
			round++; reportRound();
			return true;
		}
		
		/* If the engine is quiescent, advance all logical clocks. */
		boolean progress = false;
		
		for (LogicalClock clock : clocks) {
			progress = clock.advance() || progress;
		}
		
		/* If some logical clock actually advanced, return. */
		return progress;
	}
	
	/**
	 * Activate a token by adding it to the queue of active tokens
	 * @param t	the token to be added
	 */
	synchronized public void activate(Token t) {
		activeTokens.addLast(t);
		notifyAll();
	}
	
	/**
	 * Activate a token by adding it to the queue of returning tokens
	 * @param t	the token to be added
	 */
	synchronized public void resume(Token t) {
		queuedReturns.addLast(t);
		notifyAll();
	}
	
	/**
	 * Publish a result. This method is called by the Pub node
	 * when a publication is 'escaping' the bottom of the
	 * execution graph.
	 * 
	 * The default implementation prints the value's string
	 * representation to the console. Change this behavior
	 * by extending OrcEngine and overriding this method.
	 * 
	 * @param v
	 */
	public void pub(Value v) {
		System.out.println(v.toString());
		System.out.flush();
	}
	
	/**
	 * A token owned by this engine has encountered an exception.
	 * The token dies, remaining silent and leaving the execution,
	 * and then calls this method so that the engine can report or 
	 * otherwise handle the failure.
	 */
	public void tokenError(Token t, TokenException problem) {
		System.out.println();
		System.out.println("Token " + t + " encountered an error. ");
		System.out.println("Problem: " + problem);
		System.out.println("Source location: " + problem.getSourceLocation());
		if (debugMode) {
			problem.printStackTrace();
		}
		Throwable cause = problem.getCause();
		if (debugMode && cause != null) {
			System.out.println("Caused by:");
			cause.printStackTrace();
		}
		System.out.println();
	}
	
	
	public void debug(String s) {
		if (debugMode) System.out.println(s);
	}
	
	public void reportRound() {
		if (debugMode){
			debug("---\n" + 
			      "Round:   " + round + "\n" +
			      "Active:  " + activeTokens.size() + "\n" +
			      "Queued:  " + queuedReturns.size() + "\n");
			for(LogicalClock clock : clocks) {
			      debug("L-Clock: " + clock.getTime() + "\n");
			}
			debug("---\n\n");
		}
	}
	
	public synchronized boolean addClock(LogicalClock clock) {
		return clocks.add(clock);
	}
	/**
	 * Print something (for use by the print and println sites). By default,
	 * this prints to System.out, but this can be overridden to do something
	 * else if appropriate.
	 * 
	 * @param string
	 */
	public void print(String string) {
		System.out.print(string);
	}
	/**
	 * Print something (for use by the print and println sites). By default,
	 * this prints to System.out, but this can be overridden to do something
	 * else if appropriate.
	 * 
	 * @param string
	 */
	public void println(String string) {
		System.out.println(string);
	}
	/** Provide access to a package static method. */
	public void setCurrentToken(Token caller) {
		Continuation.setCurrentToken(caller);
	}
	/** Provide access to a package static method. */
	public Token getCurrentToken() {
		return Continuation.getCurrentToken();
	}
}
