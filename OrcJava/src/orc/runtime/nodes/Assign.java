/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.nodes;

import orc.runtime.Token;

/**
 * Compiled node for assignment. 
 * @author dkitchin, wcook
 */
public class Assign extends Node {
	private static final long serialVersionUID = 1L;
	public Node next;

	public Assign(Node next) {
		this.next = next;
	}

	/** 
	 * When executed, extends the environment with a new binding.
	 * The result value in the input token is pushed onto the env stack.
	 * The token moves to the next node and reactivates.
	 * @see orc.runtime.nodes.Node#process(orc.runtime.Token)
	 */
	public void process(Token t) {
		Object val = t.getResult();
		t.bind(val);
		t.move(next).activate();
	}

	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
