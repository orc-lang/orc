/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import orc.runtime.nodes.Node;
import orc.runtime.regions.Execution;

/**
 * The Orc Engine provides the main loop for executing active tokens.
 * @author wcook, dkitchin
 */
public class OrcEngine {

	LinkedList<Token> activeTokens = new LinkedList<Token>();
	LinkedList<Token> queuedReturns = new LinkedList<Token>();
	Set<LogicalClock> clocks = new HashSet<LogicalClock>();
	int round = 1; 
	public boolean debugMode = false;
	
	
	/**
	 * Run Orc given a root node.
	 * Creates an initial environment and then 
	 * executes the main loop.
	 * @param root  node to run
	 */ 
	public void run(Node root) {
		this.run(root, null);
	}
	
	public void run(Node root, Environment env) {
		
		Execution exec = new Execution(this);
		Token start = new Token(root, env, exec);
		activeTokens.add(start);
        
		/* Run this execution to completion. */
		while (exec.isRunning()) { step(exec); }
	}
	
	/* Returns true if the Orc engine is still executing, false otherwise */
	synchronized private boolean step(Execution exec) {
		
		if (!exec.isRunning()) { 
			return false;
		}
		
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
		if (progress) { return true; }
		
		
		/* If there is no more available work,
		 * wait for a site call to return. 
		 */
		/*
		try {
			wait();
		} 
		catch (InterruptedException e) {}
		*/
		/* Done waiting; return to work. */
		return true;
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
			      "Queued:  " + queuedReturns.size() + "\n");
			for(LogicalClock clock : clocks) {
			      debug("L-Clock: " + clock.getTime() + "\n");
			}
			debug("---\n\n");
			}
	}
	
	public boolean addClock(LogicalClock clock) {
		return clocks.add(clock);
	}

	
}
