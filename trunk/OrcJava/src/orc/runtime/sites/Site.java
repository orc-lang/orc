/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.sites;

import java.util.LinkedList;
import java.util.List;

import orc.error.JavaException;
import orc.error.OrcException;
import orc.error.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.nodes.Node;
import orc.runtime.values.Future;
import orc.runtime.values.Value;
import orc.runtime.values.Callable;
import orc.runtime.values.Visitor;

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
	 * @see orc.runtime.values.Callable#createCall(orc.runtime.Token, java.util.List, orc.runtime.nodes.Node)
	 */
	public void createCall(Token callToken, List<Future> args, Node nextNode) throws TokenException {

		List<Value> values = new LinkedList<Value>();
		
		for (Future f : args)
		{	
			Value v = f.forceArg(callToken);
			if (v == null) 
				{ return; }
			else 
				{ values.add(v); }
		}
		
		callSite(new Args(values), callToken.move(nextNode));
	}

	
	/**
	 * Must be implemented by subclasses to implement the site behavior
	 * @param args			list of argument values
	 * @param caller	where the result should be sent
	 */
	abstract public void callSite(Args args, Token caller) throws TokenException;

	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
