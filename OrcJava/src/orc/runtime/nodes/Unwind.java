/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.nodes;

import orc.ast.simple.arg.Var;
import orc.runtime.Token;
import orc.runtime.values.Value;

/**
 * Compiled node for leaving the scope of a variable binding. 
 * @author dkitchin
 */
public class Unwind extends Node {
	private static final long serialVersionUID = 1L;
	Node next;
	int width;

	public Unwind(Node next) {
		this.next = next;
		this.width = 1;
	}

	public Unwind(Node next, int width) {
		this.next = next;
		this.width = width;
	}

	/** 
	 * When executed, extends the environment with a new binding.
	 * The result value in the input token is pushed onto the env stack.
	 * The token moves to the next node and reactivates.
	 * @see orc.runtime.nodes.Node#process(orc.runtime.Token)
	 */
	public void process(Token t) {
		t.unwind(width).move(next).activate();
	}

}
