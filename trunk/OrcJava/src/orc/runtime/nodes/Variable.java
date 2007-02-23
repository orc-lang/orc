/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.nodes;

import orc.runtime.OrcEngine;
import orc.runtime.Token;
import orc.runtime.values.GroupCell;
import orc.runtime.values.Value;
import java.io.*;

/**
 * A compiled variable node
 * @author wcook
 */
public class Variable implements Param, Serializable {
	private static final long serialVersionUID = 1L;
	String var;

	public Variable(String var) {
		this.var = var;
	}

	/** 
	 * Looks up the variable to see if it is bound.
	 * If the variable is bound to a constant, then it will
	 * never be unbound. If the variable is associated with a group,
	 * then it may be unbound.
	 * If the group is unbound, then the input token is added to the
	 * waiting queue for the group.
	 * @see orc.runtime.nodes.Param#waitOnUnboundVar(orc.runtime.Token)
	 */
	public boolean waitOnUnboundVar(Token t,OrcEngine engine) {
		Value holder = t.lookup(var);
		GroupCell cell = holder.asUnboundCell();
		if (cell == null)
			return false;
		cell.waitForValue(t);
		return true;
	}

	/** 
	 * Looks up the value in of the variable in the environment.
	 * @see orc.runtime.nodes.Param#getValue(orc.runtime.Token)
	 */
	public Value getValue(Token env) {
		return env.lookup(var);
	}
	
	public String toString() {
		return "var{" + var + "}";
	}
}
