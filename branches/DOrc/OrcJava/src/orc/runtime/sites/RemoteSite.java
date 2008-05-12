/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.sites;

import java.rmi.Remote;
import java.rmi.RemoteException;

import orc.error.OrcRuntimeTypeException;
import orc.runtime.Args;
import orc.runtime.RemoteToken;

/**
 * Base class for all sites
 * @author quark
 */
public interface RemoteSite extends Remote {
	public void callRemoteSite(Args args, RemoteToken caller) throws RemoteException, OrcRuntimeTypeException;
}
