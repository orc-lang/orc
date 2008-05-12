/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.values;

import java.lang.reflect.Proxy;
import java.rmi.RemoteException;
import java.rmi.server.ExportException;
import java.rmi.server.UnicastRemoteObject;
import java.util.LinkedList;
import java.util.List;

import orc.error.OrcException;
import orc.error.OrcRuntimeTypeException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.nodes.Node;
import orc.runtime.sites.RemoteSite;

/**
 * Wrap a site implementation in a serializable value. This is necessary in
 * order to guarantee that all values are passed by value in a distributed
 * computation, although they may (as in the case of a mutable site) hold a
 * reference to something which is not. This avoids tainting too much code with
 * RMI details.
 * 
 * Is it confusing to have both Site (a value) and Site (a site implementation)?
 * Maybe I should have called this SiteValue.
 * 
 * @author quark
 */
public class Site extends Value implements Callable {
	protected RemoteSite site;

	public Site(RemoteSite site) {
		this.site = site;
	}

	/** 
	 * Invoked by a Call to invoke a site. The argument list is 
	 * scanned to make sure that all parameters are bound.
	 * If an unbound parameter is found, the call is placed on a 
	 * queue and nothing more is done.
	 * Once all parameters are bound, their values are collected
	 * and the corresponding subclass (the actual site) is called. 
	 * 
	 * @see orc.runtime.values.Callable#createCall(orc.runtime.Token, java.util.List, orc.runtime.nodes.Node)
	 */
	public void createCall(Token callToken, List<Future> args, Node nextNode) throws OrcException {
		List<Value> values = new LinkedList<Value>();
		try {
			for (Future f : args) values.add(f.forceArg(callToken));
		} catch (FutureUnboundException e) {
			return;
		}

		final Token caller = callToken.move(nextNode);
		final Args siteArgs = new Args(values);
		if (site instanceof Proxy) {
			try {
				// Register the caller so it can receive results
				// from the site.
				UnicastRemoteObject.exportObject(caller, 0);
			} catch (ExportException e) {
				// We assume this means that the object was already
				// exported, so we can ignore this error.
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				throw new RuntimeException(e);
			}
			// Calls to remote sites must be run in a separate
			// thread to prevent deadlock (which occurs if the
			// remote site tries to return a value immediately).
			new Thread(new Runnable() {
				public void run() {
					try {
						site.callRemoteSite(siteArgs, caller);
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						throw new RuntimeException(e);
					} catch (OrcRuntimeTypeException e) {
						caller.error(e);
					}
				}
			}).start();
		} else {
			try {
				site.callRemoteSite(siteArgs, caller);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				throw new RuntimeException(e);
			}
		}
			
	}
}
