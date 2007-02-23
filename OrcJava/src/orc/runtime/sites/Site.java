/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.sites;

import java.util.List;
import java.io.File;

import orc.runtime.OrcEngine;
import orc.runtime.Token;
import orc.runtime.nodes.Node;
import orc.runtime.nodes.Param;
import orc.runtime.values.BaseValue;
import orc.runtime.values.Callable;
import orc.runtime.values.GroupCell;
import orc.runtime.values.Tuple;

/**
 * Base class for all sites
 * @author wcook
 */
public abstract class Site extends BaseValue implements Callable {

	public boolean Callable0(){
		return false;  //overridden by sites that can be called with no arguments.
	}
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
			if (e.waitOnUnboundVar(callToken,engine)) {
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

		callSite(values, callToken.move(nextNode), callToken.getGroup(), engine);
	}

	/**
	 * Must be implemented by subclasses to implement the site behavior
	 * @param args			list of argument values
	 * @param returnToken	where the result should be sent
	 * @param engine		Orc engine -- used for suspending or activating tokens
	 */
	abstract public void callSite(Object[] args, Token returnToken, GroupCell caller,
			OrcEngine engine);

	
	/**
	 * Helper function to retrieve the nth element, with error checking
	 */
	public Object getArg(Object[] args, int n)
	{
		try
		 	{ return args[n]; }
		catch (ArrayIndexOutOfBoundsException e)
			{ throw new Error("Arity mismatch calling site '" + this.toString() + "'. Could not find argument #" + n); }
	}
	
		
	/**
	 * Helper function for integers
	 */
	public int intArg(Object[] args, int n) {
		
		Object a = getArg(args,n);
		try
			{ return ((Integer)a).intValue(); }
		catch (ClassCastException e) 
			{ throw new Error("Argument " + n + " to site '" + this.toString() + "' should be an int, got " + a.getClass().toString() + " instead."); } 
	}

	/**
	 * Helper function for booleans
	 */
	public boolean boolArg(Object[] args, int n) {
		
		Object a = getArg(args,n);
		try
			{ return ((Boolean)a).booleanValue(); }
		catch (ClassCastException e) 
			{ throw new Error("Argument " + n + " to site '" + this.toString() + "' should be a boolean, got " + a.getClass().toString() + " instead."); } 
	
	}

	/**
	 * Helper function for strings
	 */
	public String stringArg(Object[] args, int n) {

		Object a = getArg(args,n);
		return a.toString();
	}
	
	/**
	 * Helper function for Files
	 * Removed: files are not ground values.
	 */
	/*
	public File fileArg(Object[] args, int n) {

		Object a = getArg(args,n);
		try
			{ return (File)a; }
		catch (ClassCastException e) 
			{ throw new Error("Argument " + n + " to site '" + this.toString() + "' should be a file, got " + a.getClass().toString() + " instead."); } 
	
	}
	*/
	
	/**
	 * Helper function to access the canonical 'signal' value
	 * 
	 * Currently, the canonical value is an empty tuple
	 */
	public Object signal() {
		return new Tuple(new Object[0]);
	}
}
