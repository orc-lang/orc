/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime;

import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import orc.runtime.nodes.Node;
import orc.runtime.regions.Execution;
import orc.runtime.regions.Region;
import orc.runtime.values.GroupCell;

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
	private boolean halt = false;

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
		notify();
	}

	/**
	 * Run Orc given a root node.
	 * Creates an initial environment and then 
	 * executes the main loop.
	 * @param root  node to run
	 */ 
	public void run(Node root) {
		run(root, null);
	}

	public void run(Node root, Environment env) {
		activate(new Token(root, env, null, new Group(), new Execution(this), null, this));
		run();
	}
	
	/**
	 * Run one step (process one token, handle one site response, or advance
	 * all logical clocks). Returns true if work was done.
	 */
	private boolean step() {
		/* If an active token is available, process it. */
		if (activeTokens.size() > 0){
			activeTokens.remove().process();
			return true;
		}
		
		/* If a site return is available, make it active.
		 * This marks the beginning of a new round. 
		 */
		if (queuedReturns.size() > 0 ){
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
		notify();
	}
	
	/**
	 * Activate a token by adding it to the queue of returning tokens
	 * @param t	the token to be added
	 */
	synchronized public void resume(Token t) {
		queuedReturns.addLast(t);
		notify();
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
}
