/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.nodes;

import orc.runtime.OrcEngine;
import orc.runtime.Token;
import orc.runtime.values.Constant;
import orc.runtime.values.Value;

/**
 * A compiled literal node
 * @author wcook
 */
public class Literal extends Node implements Param {

	Object value;
	Node next;
	
	public Literal(Object value, Node next) {
		this.value = value;
		this.next = next;
	}

	/**
	 * Executing a literal sets the value of the token and then activates the next node. 
	 * @see orc.runtime.nodes.Node#process(orc.runtime.Token, orc.runtime.OrcEngine)
	 */
	public void process(Token t, OrcEngine engine) {
		t.setResult(value);
		engine.activate( t.move(next) );
	}

	/**
	 * Creates a constant container for the literal value  
	 * @see orc.runtime.nodes.Param#getValue(orc.runtime.Token)
	 */
	public Value getValue(Token env) {
		return new Constant(value);
	}

	/**
	 * Literals are never unbound. 
	 * @see orc.runtime.nodes.Param#waitOnUnboundVar(orc.runtime.Token)
	 */
	public boolean waitOnUnboundVar(Token env) {
		return false;
	}

	public String toString() {
		if (value instanceof String)
			return "\"" + value + "\"";
		else
			return value.toString();
	}
}
