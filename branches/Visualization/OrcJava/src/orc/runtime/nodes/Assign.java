/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.nodes;

import orc.ast.simple.arg.Var;
import orc.runtime.Token;
import orc.runtime.values.Value;

/**
 * Compiled node for assignment. 
 * @author wcook
 */
public class Assign extends Node {
	private static final long serialVersionUID = 1L;
	Var var;
	Node next;

	public Assign(Var var, Node next) {
		this.var = var;
		this.next = next;
	}

	/** 
	 * When executed, extends the environment with a new binding.
	 * The result value in the input token is bound to the variable name.
	 * The token moves to the next node and reactivates.
	 * @see orc.runtime.nodes.Node#process(orc.runtime.Token)
	 */
	public void process(Token t) {
		
		t.debug("Assign " + var + "=" + t.getResult());

		Value val = t.getResult();
		t.bind(var, val);
		t.move(next).activate();
	}

}
