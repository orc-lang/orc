/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.nodes;

import java.util.List;

import orc.runtime.OrcEngine;
import orc.runtime.Token;
import orc.runtime.values.Callable;
import orc.runtime.values.Value;

/**
 * Compiled node for a call (either a site call or a definition call)
 * @author wcook
 */
public class Call extends Node {

	String name;
	List<Param> args;
	Node next;

	public Call(String name, List<Param> args, Node next) {
		this.name = name;
		this.args = args;
		this.next = next;
	}

	/** 
	 * Looks up the function to be called, then creates a call
	 * token using the argument expressions.
	 * TODO: why does this check for callable?
	 * @see orc.runtime.nodes.Node#process(orc.runtime.Token, orc.runtime.OrcEngine)
	 */
	public void process(Token t, OrcEngine engine) {

		Value d = t.lookup(name);
		// define call with return location
		if (d instanceof Callable ) 
		{
			Callable target = (Callable) d;
			target.createCall(name, t, args, next, engine);
		}
		else if(d.asBasicValue() instanceof Callable)
		{
			Callable target = (Callable) d.asBasicValue();
			target.createCall(name, t, args, next, engine);			
		}
		else
			t.setResult(d);
	}
}
