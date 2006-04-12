/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.nodes;

import orc.runtime.OrcEngine;
import orc.runtime.Token;
import orc.runtime.values.Constant;

/**
 * Compiled node for assignment. 
 * @author wcook
 */
public class Assign extends Node {
	String var;
	Node next;

	public Assign(String var, Node next) {
		this.var = var;
		this.next = next;
	}

	/** 
	 * When executed, extends the environment with a new binding.
	 * The result value in the input token is bound to the variable name.
	 * The next node is activated.
	 * @see orc.runtime.nodes.Node#process(orc.runtime.Token, orc.runtime.OrcEngine)
	 */
	public void process(Token t, OrcEngine engine) {
		if (engine.debugMode)
			engine.debug("Assign " + var + "=" + t.getResult(), t);

		Object val = t.getResult();
		t.bind(var, new Constant(val));
		engine.activate(t.move(next));
	}
}
