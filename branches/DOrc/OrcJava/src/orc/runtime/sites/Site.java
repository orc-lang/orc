/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.sites;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import orc.error.OrcRuntimeTypeException;
import orc.runtime.Args;
import orc.runtime.RemoteToken;

/**
 * Base class for all sites.
 *
 * If your class implements the PassedByValueSite interface, it will be
 * copied between distributed nodes rather than shared.
 *
 * @author wcook
 */
public abstract class Site implements RemoteSite {
	public Site() {
		// Allow this site to be called remotely, if
		// it is not a "pure" (stateless) site which
		// can be copied between nodes.
		if (!(this instanceof PassedByValueSite)) {
			try {
				UnicastRemoteObject.exportObject(this, 0);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Synchronize accesses to the site if appropriate.
	 */
	public void callRemoteSite(Args args, RemoteToken caller) throws OrcRuntimeTypeException, RemoteException {
		if (this instanceof PassedByValueSite) {
			callSite(args, caller);
		} else {
			// remote calls should be synchronized
			synchronized (this) {
				callSite(args, caller);
			}
		}
	}
	
	/**
	 * Must be implemented by subclasses to implement the site behavior
	 * @param args			list of argument values
	 * @param caller	where the result should be sent
	 */
	abstract public void callSite(Args args, RemoteToken caller) throws OrcRuntimeTypeException, RemoteException;
}
