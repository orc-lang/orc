/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime;

import java.util.LinkedList;

import orc.runtime.nodes.Node;
import orc.runtime.regions.Execution;

/**
 * The Orc Engine provides the main loop for executing active tokens.
 * @author wcook, dkitchin
 */
public class OrcEngine {

	LinkedList<Token> activeTokens = new LinkedList<Token>();
	LinkedList<Token> queuedReturns = new LinkedList<Token>();
	
	//number of values published by the root node.
	//updated by PrintResult and WriteResult.
	int num_published = 0;
	// If max_publish is non null, then Orc should exit after publishing that many values.
	Integer max_publish = null;
	
	LogicalClock clock;
	
	int round = 1; 
	
	public boolean debugMode = false;
	
	public OrcEngine(Integer pub){
		this.max_publish = pub;
		this.clock = new LogicalClock();
	}

	/**
	 * Run Orc given a root node.
	 * Creates an initial environment and then 
	 * executes the main loop.
	 * @param root  node to run
	 */
	public void run(Node root, Environment env) {
		
		Execution exec = new Execution(this);
		Token start = new Token(root, env, exec);
		activeTokens.add(start);
        work(exec);
	}
	
	public void work(Execution exec) {
	   
		while (moreWork(exec)) {
			
			/* If an active token is available, process it. */
			if (activeTokens.size() > 0){
				activeTokens.remove().process();
				continue;
			}
			
			/* If a site return is available, make it active.
			 * This marks the beginning of a new round. 
			 */
			if (queuedReturns.size() > 0 ){
				activeTokens.add(queuedReturns.remove());
				round++; reportRound();
				continue;
			}
			
			/* Attempt to advance the logical clock. */
			if (clock.advance()) { continue; }
		}
	}
	
	/**
	 * Internal function to check if there is more work to do
	 * @return true if more work
	 */
	private synchronized boolean moreWork(Execution exec) {
		if (max_publish != null && max_publish.intValue() <= num_published)
			return false;
		
		/* If this execution has been halted, return false immediately.
		 * 
		 * If there are no active or queued tokens,
		 * advance the logical clock to its next time point,
		 * making all of the logical timer calls for that time
		 * point available as site returns. 
		 * 
		 * If the logical clock cannot be advanced, suspend the engine.  
		 * 
		 * Note that ordinary site returns are considered to take zero logical time,
		 * so any expression waiting on the logical clock may experience starvation
		 * if there are an infinite number of rounds taking zero logical time.
		 */
		if (exec.isRunning() && activeTokens.size() == 0 && queuedReturns.size() == 0 && clock.stuck()) {
			/* There is no more work available. Wait for a site call to return */ 
			try {
				wait();
			} catch (InterruptedException e) {}
		}
		return exec.isRunning();
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
	 * Force the engine to wake up any threads waiting in work loops.
	 * This is used to make a thread aware of the completion of its execution
	 * without knowing the identity of that thread.
	 */
	synchronized public void wake() {
		notifyAll();
	}
	
	public void addPub(int n) {
		num_published += n;
	}

	public void debug(String s)
	{
		if (debugMode)
			{ System.out.println(s); }
	}
	
	public void reportRound() {
		if (debugMode){
			debug("---\n" + 
			      "Round:   " + round + "\n" +
			      "Active:  " + activeTokens.size() + "\n" +
			      "Queued:  " + queuedReturns.size() + "\n" +
			      "L-Clock: " + clock.getTime() + "\n" +
			      "---\n\n");
			}
	}
	
	public LogicalClock getClock() { return clock; }
	
}
