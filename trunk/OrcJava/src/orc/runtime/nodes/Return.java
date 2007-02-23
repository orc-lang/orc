/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.nodes;

import orc.runtime.OrcEngine;
import orc.runtime.Token;

/**
 * Compiled node marking the end of a procedure
 * @author wcook
 */
public class Return extends Node {
	private static final long serialVersionUID = 1L;

	/**
	 * To execute a return, the caller token and the result of the current
	 * execution are identified.
	 * The caller token points to the node after the call.  
	 * The caller is then copied, the result of the caller is set, and 
	 * the token is activated.
	 * @see orc.runtime.nodes.Node#process(orc.runtime.Token, orc.runtime.OrcEngine)
	 */
	public void process(Token t, OrcEngine engine) {
		if (engine.debugMode)
			engine.debug("Return " + t.getResult(), t);
		
		Token caller = t.getCaller();
		Object result = t.getResult();
		engine.activate(caller.copy().setResult(result));
	}
}
