/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.sites;

import java.util.LinkedList;
import java.util.List;

import orc.runtime.OrcEngine;
import orc.runtime.Token;
import orc.runtime.nodes.Node;
import orc.runtime.values.Future;
import orc.runtime.values.Value;
import orc.runtime.values.Callable;
import orc.runtime.values.GroupCell;
import orc.runtime.values.Tuple;

/**
 * Base class for all sites
 * @author wcook
 */
public abstract class Site extends Value implements Callable {

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
	public void createCall(Token callToken, List<Future> args, Node nextNode, OrcEngine engine) {

		List<Value> values = new LinkedList<Value>();
		
		for (Future f : args)
		{	
			Value v = f.forceArg(callToken);
			if (v == null) 
				{ return; }
			else 
				{ values.add(v); }
		}
		
		callSite(new Tuple(values), callToken.move(nextNode), callToken.getGroup(), engine);
	}

	
	/**
	 * Must be implemented by subclasses to implement the site behavior
	 * @param args			list of argument values
	 * @param returnToken	where the result should be sent
	 * @param engine		Orc engine -- used for suspending or activating tokens
	 */
	abstract public void callSite(Tuple args, Token returnToken, GroupCell caller, OrcEngine engine);

	
	
	
	/**
	 * Helper function to access the canonical 'signal' value
	 * 
	 * Currently, the canonical value is an empty tuple
	 */
	public Value signal() {
		return new Tuple(new LinkedList<Value>());
	}
}
