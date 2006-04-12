/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.sites;

import java.util.List;

import orc.runtime.OrcEngine;
import orc.runtime.Token;
import orc.runtime.nodes.Node;
import orc.runtime.nodes.Param;
import orc.runtime.values.BaseValue;
import orc.runtime.values.Callable;
import orc.runtime.values.Tuple;

/**
 * Base class for all sites
 * @author wcook
 */
public abstract class Site extends BaseValue implements Callable {

	/** 
	 * Invoked by a Call to invoke a site. The argument list is 
	 * scanned to make sure that all parameters are bound.
	 * If an unbound parameter is found, the call is placed on a 
	 * queue and nothing more is done.
	 * Once all parameters are bound, their values are collected
	 * and the corresponding subclass (the actual site) is called. 
	 * 
	 * @see orc.runtime.values.Callable#createCall(java.lang.String, orc.runtime.Token, java.util.List, orc.runtime.nodes.Node, orc.runtime.OrcEngine)
	 */
	public void createCall(String label, Token callToken,
			List<Param> args, Node nextNode, OrcEngine engine) {

		for (Param e : args)
			if (e.waitOnUnboundVar(callToken)) {
				if (engine.debugMode)
					engine.debug("Wait " + label + " for " + e, callToken);
				return;
			}

		Object[] values = new Object[args.size()];
		int i = 0;
		for (Param e : args)
			values[i++] = e.getValue(callToken).asBasicValue();

		if (engine.debugMode)
			engine.debug("Call site " + label + new Tuple(values), callToken);

		callSite(values, callToken.move(nextNode), engine);
	}

	/**
	 * Must be implemented by subclasses to implement the site behavior
	 * @param args			list of argument values
	 * @param returnToken	where the result should be sent
	 * @param engine		Orc engine -- used for suspending or activating tokens
	 */
	abstract void callSite(Object[] args, Token returnToken,
			OrcEngine engine);

	/**
	 * Helper function for integers
	 */
	int intArg(Object[] args, int n) {
		return ((Integer)args[n]).intValue();
	}

	/**
	 * Helper function for booleans
	 */
	boolean boolArg(Object[] args, int n) {
		return ((Boolean)args[n]).booleanValue();
	}

	/**
	 * Helper function for strings
	 */
	String stringArg(Object[] args, int n) {
		return args[n].toString();
	}
}
