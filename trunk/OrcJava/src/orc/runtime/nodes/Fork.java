/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.nodes;

import orc.runtime.Token;

/**
 * A compile node that performs a fork to run two subnodes.
 * @author wcook
 */
public class Fork extends Node {
	private static final long serialVersionUID = 1L;
	Node left;
	Node right;
	public Fork(Node left, Node right) {
		this.left = left;
		this.right = right;
	}

	/** 
	 * The input token is activated on the right node,
	 * and a copy is activated on the left node.
	 */
	public void process(Token t) {
		/*
		if (engine.debugMode)
			engine.debug("Fork", t);
		*/
		t.copy().move(left).activate();
		t.move(right).activate();
	}

}
