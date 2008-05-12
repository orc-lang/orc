/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.nodes;


import orc.ast.simple.arg.Var;
import orc.runtime.Group;
import orc.runtime.Token;
import orc.runtime.values.GroupCell;
import orc.runtime.values.Value;

/**
 * Compiled node used to store the value of a binding in a where clause.
 * @author wcook
 */
public class Store extends Node {
	private static final long serialVersionUID = 1L;
	Var var;
	public Store(Var var) {
		this.var = var;
	}

	/**
	 * Gets the group of the token and sets its value to be the result
	 * of the input token. 
	 * As a side effect of setting the value of a group, a "where" variable
	 * becomes bound and the execution of the group is suspended.
	 * @see orc.runtime.nodes.Node#process(orc.runtime.Token, orc.runtime.OrcEngine)
	 */
	public void process(Token t) {
		/*
		if (engine.debugMode)
			engine.debug("Store/Stop " + var + "=" + t.getResult(), t);
		*/
		t.getGroup().setValue(t.getResult());
		t.die();
	}
	public String toString() {
		return super.toString() + "(" + var +")";
	}
}
