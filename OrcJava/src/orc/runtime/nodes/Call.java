/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.nodes;

import java.util.List;

import orc.runtime.OrcEngine;
import orc.runtime.Token;
import orc.runtime.sites.java.ObjectProxy;
import orc.runtime.values.Callable;
import orc.runtime.values.Value;

/**
 * Compiled node for a call (either a site call or a definition call)
 * @author wcook
 */
public class Call extends Node {
	private static final long serialVersionUID = 1L;
	String name;
	List<Param> args;
	antlr.Token tok;
	Node next;

	public Call(String name, List<Param> args, Node next, antlr.Token t) {
		this.name = name;
		this.args = args;
		this.next = next;
		this.tok = t;
		
	}
	
	public String Label() {
		if (tok == null)
			return name;
		return name + " on line " + tok.getLine();
	}

	/** 
	 * Looks up the function to be called, then creates a call
	 * token using the argument expressions.
	 * TODO: why does this check for callable?
	 * @see orc.runtime.nodes.Node#process(orc.runtime.Token, orc.runtime.OrcEngine)
	 */
	// TODO FIX BUG: Can't use blocked variables as callable entities
	public void process(Token t, OrcEngine engine) {
		
		
		Value d = t.lookup(name);
		
		// define call with return location
	    if (d instanceof Callable ) {
			Callable target = (Callable) d;
			target.createCall(Label(), t, args, next, engine);
		}
	    else 
	    {
	    	Object v = d.asBasicValue();
	    
	    	if (v instanceof Callable) {
	    		Callable target = (Callable) v;
	    		target.createCall(Label(), t, args, next, engine);
	    	}
	    	/**
		     * TODO: Fix this in a principled way. This is currently a hack that autoconverts ground
		     * values v in call position to the equivalent of let(v).
		     * 
		     * The correct solution is to apply this conversion earlier in the compilation process,
		     * probably to the AST as a normalization step.
		     * 
		     */ 
	    	else
	    	{
	    		t.setResult(v);
				t.move(next);
				engine.activate(t);
	    	}	
	    	/*
	    	else if (v instanceof Integer || v instanceof Boolean || v instanceof String)
	    	{
	    		t.setResult(v);
				t.move(next);
				engine.activate(t);
	    	}
	    	else
	    	{
	    		// This is an open object instance, and should be proxied.
	    		ObjectProxy target = new ObjectProxy(v);
	    		target.createCall(Label(), t, args, next, engine);
	    	}
	    	*/
	    }
	}
	
}
