/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime;

import orc.runtime.values.Value;
import java.io.*;

/**
 * Lexical environment containing variable bindings
 * @author wcook
 */
public class Environment implements Serializable {
	private static final long serialVersionUID = 1L;
	Environment parent;
	String var;
	Value value;

	public Environment(String var, Value value, Environment parent) {
		this.var = var;
		this.value = value;
		this.parent = parent;
	}

	/**
	 * Lookup a variable in the environment
	 * TODO: should be compiled using activation frames and variable offsets.
	 * Currently uses a linear search.
	 * @param var	variable name
	 * @return		value, or error if binding exists
	 */
	public Value lookup(String var) {
		if (this.var.equals(var))
			return value;
		else if (parent == null)
			throw new Error("Undefined variable: " + var);
		else
			return parent.lookup(var);
	}
	
	/* 
	 * This is something I tried, to see whether I could save memory in some recursions.
	 * But it didn't help.  -- Mark B
	public Environment destructiveSet(String var, Value val) {
		if (this.var.equals(var))
			this.value = val;
		else 
			this.parent = parent.destructiveSet(var,val);
		return this;
			
	}
	*/ 
}
