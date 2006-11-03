/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.sites;

import java.util.PriorityQueue;

import orc.runtime.OrcEngine;
import orc.runtime.Token;
import orc.runtime.values.Tuple;

/**
 * Implements the RTimer site
 *  * @author wcook
 */
public class Rtimer extends Site {

	/** 
	 * When called, the RTimer creates a new thread which wakes up after some time and returns the value
	 * @see orc.runtime.sites.Site#callSite(java.lang.Object[], orc.runtime.Token, orc.runtime.OrcEngine)
	 * 
	 */
	public static JavaTimer javaTimer;
	boolean absolute;
	public Rtimer(boolean absolute)
	{
		this.absolute = absolute;
	}

	public void callSite(Object[] args, Token returnToken, OrcEngine engine) 
	{
		if (args.length != 1 || !(args[0] instanceof Long))
			throw new Error("Invalid argument in Rtimer" + args);
		engine.addCall(1);

		long n = ((Long) args[0]).longValue();
		if (javaTimer == null)
			javaTimer = new JavaTimer(engine);
		if (!absolute)
			n += System.currentTimeMillis();
		javaTimer.addEvent(n, returnToken);
	}
}


/**
 * Helper class that runs the actual timer and calls Rtimer Events 
 * @author jayeshs
 */

class JavaTimer implements Runnable {

	/**
	 * JavaTimer is instantiated as a static object javaTimer in  orc.runtime.sites.Rtimer. 
	 * It creates a PriorityQueue object , which is used to store events scheduled in relative time 
	 * by calls to Rtimer, and spawns a thread to remove events from the queue and return an engine call.
	 */

	Thread t;
	PriorityQueue<RtimerQueueEntry> rtimerEventQueue;
	OrcEngine engine;

	public JavaTimer(OrcEngine engine) 
	{
		this.engine = engine;
		rtimerEventQueue = new PriorityQueue<RtimerQueueEntry>();
	}
	
	public void addEvent(long time, Token token) 
	{
		if (t == null) {
			t = new Thread(this);
			t.start();
			if (engine.debugMode)
				engine.debug("Rtimer: Starting Timer Thread.",token);

		}
		long at = time;
		rtimerEventQueue.add(new RtimerQueueEntry(at, token));
		if (engine.debugMode)
			engine.debug("Rtimer: Adding event to Rtimer Event Queue.",token);
		t.interrupt();

	}

	public synchronized void run() {
		while (true) {
			try 
			{
				RtimerQueueEntry temp = rtimerEventQueue.peek();
				if (temp == null)
				{
					// wait until interrupted
					wait();
				}
				else if (temp.getTime() > System.currentTimeMillis())
				{
					// wait for first event
					if (engine.debugMode)
						engine.debug("Rtimer: Waiting for " + (temp.getTime() - System.currentTimeMillis()),temp.getToken());
					wait(temp.getTime() - System.currentTimeMillis());
				}
				else
				{
					// execute the event
					rtimerEventQueue.remove();
					engine.addCall(-1);
					if (engine.debugMode)
						engine.debug("Rtimer: Executed Event.",temp.getToken());
					
					engine.siteReturn("Rtimer", temp.getToken(), true);
										
				}
			} 
			catch(InterruptedException e) 
			{
				/*something added to queue */

			}
		
		}
	}



	/**
	 * Class representing Rtimer Queue Entry 
	 * @author jayeshs
	 */
	class RtimerQueueEntry implements Comparable<RtimerQueueEntry> {

		long time;
		Token token;

		public RtimerQueueEntry(long time, Token token) {
			this.token = token;
			this.time = time;
		}
		public long getTime() {
			return time;
		}
		public Token getToken() {
			return token;
		}

		// sort the queue items earliest first
		public int compareTo(RtimerQueueEntry n) {
			long diff = time - n.time;

			if (diff == 0)
				return 0;
			else if (diff > 0)
				return 1;
			else
				return -1;
		}
	}	
}

