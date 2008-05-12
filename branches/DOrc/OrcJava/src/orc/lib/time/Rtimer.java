/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.lib.time;

import java.rmi.RemoteException;
import java.util.Timer;
import java.util.TimerTask;

import orc.error.OrcRuntimeTypeException;
import orc.runtime.Args;
import orc.runtime.RemoteToken;
import orc.runtime.sites.Site;

/**
 * Implements the RTimer site
 * @author wcook, quark, dkitchin
 */
public class Rtimer extends Site {
	private static final long serialVersionUID = 1L;

	/** Scheduler thread for events */
	private final Timer timer = new Timer();

	public void callSite(Args args, final RemoteToken returnToken) throws OrcRuntimeTypeException {
		timer.schedule(new TimerTask() {
			public void run() {
				try {
					returnToken.resume();
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					throw new RuntimeException(e);
				}
			}
		}, args.longArg(0));
	}
}