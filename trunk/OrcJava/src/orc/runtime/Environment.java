/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime;

import orc.ast.simple.arg.Var;
import orc.runtime.values.Future;
import java.io.*;

/**
 * Lexical environment containing variable bindings
 * @author wcook
 */
public class Environment implements Serializable {
	private static final long serialVersionUID = 1L;
	Environment parent;
	Var var;
	Future value;

	public Environment(Var var, Future value, Environment parent) {
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
	public Future lookup(Var var) {
		if (this.var.equals(var))
			return value;
		else if (parent == null)
			throw new Error("Undefined variable: " + var);
		else
			return parent.lookup(var);
	}
	
}
