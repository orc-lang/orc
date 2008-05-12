/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.nodes;

import java.rmi.RemoteException;

import orc.error.OrcException;
import orc.runtime.Token;

/**
 * Compiled node marking the end of a procedure
 * @author wcook
 */
public class Return extends Node {
	/**
	 * To execute a return, the caller token and the result of the current
	 * execution are identified.
	 * The caller token points to the node after the call.  
	 * The caller is then copied, the result of the caller is set, and 
	 * the token is activated.
	 * @see orc.runtime.nodes.Node#process(orc.runtime.Token, orc.runtime.OrcEngine)
	 */
	public void process(Token t) {
		/*
		if (engine.debugMode)
			engine.debug("Return " + t.getResult(), t);
		*/
		try {
			t.getCaller().returnResult(t.getResult());
		} catch (RemoteException e) {
			t.error(new OrcException(e));
		}
		t.die();
	}
}