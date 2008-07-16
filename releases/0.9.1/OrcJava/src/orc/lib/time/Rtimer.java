/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.lib.time;

import orc.error.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.Site;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Implements the RTimer site
 * @author wcook, quark, dkitchin
 */
public class Rtimer extends Site {
	private static final long serialVersionUID = 1L;

	/** Scheduler thread for events */
	private final Timer timer = new Timer();

	public void callSite(Args args, final Token returnToken) throws TokenException {
		timer.schedule(new TimerTask() {
			public void run() {
				returnToken.resume();
			}
		}, args.longArg(0));	
	}

}