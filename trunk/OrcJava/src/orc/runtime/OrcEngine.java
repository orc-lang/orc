/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime;

import java.util.LinkedList;

import orc.runtime.nodes.Node;
import orc.runtime.values.GroupCell;

/**
 * The Orc Engine provides the main loop for executing active tokens.
 * @author wcook
 */
public class OrcEngine {

	LinkedList<Token> activeTokens = new LinkedList<Token>();
	LinkedList<Token> queuedReturns = new LinkedList<Token>();
	
	int calls;
	//number of values published by the root node.
	//updated by PrintResult and WriteResult.
	int num_published = 0;
	// If max_publish is non null, then orc should exit after publishing that many values.
	Integer max_publish = null;
	public boolean debugMode = false;
	
	LogicalClock clock;
	
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

		GroupCell startGroup = new GroupCell();
		Token start = new Token(root, env, null/* caller */, startGroup, null/* value */, this);

		activeTokens.add(start);
        work();
	}
	
	public void work(){	
	    int round = 1;
		while (moreWork()) {
			
			/* If an active token is available, process it. */
			if (activeTokens.size() > 0){
				activeTokens.remove().process();
				continue;
			}
			
			// There are no active tokens available.
			
			/* If a site return is available, make it active.
			 * This marks the beginning of a new round. 
			 */
			if (debugMode){
				debug("** Round,active,queued,calls " + (round++) + 
						"," + activeTokens.size() +","+queuedReturns.size() + "," + calls +
						" ***");
				}
			
			
			if (queuedReturns.size() > 0 ){
				activeTokens.add(queuedReturns.remove());
				continue;
			}
			
			// There are no site returns available.
			
			
		}
	}
	
	/**
	 * Internal function to check if there is more work to do
	 * @return true if more work
	 */
	private synchronized boolean moreWork() {
		if (max_publish != null && max_publish.intValue() <= num_published)
			return false;
		
		/* If there are no active or queued tokens,
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
		if (activeTokens.size() == 0 && queuedReturns.size() == 0 && !clock.advance()) {
			/* There is no more work available. Wait for a site call to return */ 
			try {
				wait();
			} catch (InterruptedException e) {}
		}
		return true;
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
	
	
	public void addPub(int n) {
		num_published += n;
	}

	public void debug(String s)
	{
		if (debugMode)
			{ System.out.println(s); }
	}
	
	public LogicalClock getClock() { return clock; }
	
	/* Read a token from a dump file. */
	/*
	public Token readToken(File dump){
	 Token val = null;
	 try {
	    FileInputStream fis = new FileInputStream(dump);
        ObjectInputStream in = new ObjectInputStream(fis);
    	val = (Token) in.readObject();
    	in.close();
        fis.close();
        
        }
    catch (Exception e) {
    	System.exit(1);
    	}
    return val;
	}
	*/
	/**
	 * Run Orc given a dump file containing a single active token.
	 * @param dump file to read active token
	 */
	/*
	public void run(File dump) {
		activeTokens = new LinkedList<Token>();
		Token val = readToken(dump);
		activeTokens.add(val);
        work();
	}
	*/
	
}
