/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.sites.core;

import java.rmi.RemoteException;

import orc.runtime.Args;
import orc.runtime.RemoteToken;
import orc.runtime.sites.PassedByValueSite;
import orc.runtime.sites.Site;

/**
 * Implements the built-in "let" site. See also orc.runtime.nodes.Let,
 * which implements this more directly.
 * @author wcook
 */
public class Let extends Site implements PassedByValueSite {
	/**
	 *  Outputs a single value or creates a tuple.
	 * @throws RemoteException 
	 * @see orc.runtime.sites.Site#callSite(java.lang.Object[], orc.runtime.Token, orc.runtime.OrcEngine)
	 */
	public void callSite(Args args, RemoteToken caller) throws RemoteException {
		// A let does not resume like a normal site call; it sets the
		// result directly like a function call.
		caller.returnResult(args.condense());
		// We know the caller won't be receiving any more returns.
		caller.die();
	}
}