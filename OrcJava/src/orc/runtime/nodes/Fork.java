/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.nodes;

import orc.runtime.OrcEngine;
import orc.runtime.Token;

/**
 * A compile node that performs a fork to run two subnodes.
 * @author wcook
 */
public class Fork extends Node {
	Node left;
	Node right;
	public Fork(Node left, Node right) {
		this.left = left;
		this.right = right;
	}

	/** 
	 * The input token is activated on the right node,
	 * and a copy is activated on the left node.
	 * @see orc.runtime.nodes.Node#process(orc.runtime.Token, orc.runtime.OrcEngine)
	 */
	public void process(Token t, OrcEngine engine) {
		if (engine.debugMode)
			engine.debug("Fork", t);
		
		engine.activate(t.copy().move(left));
		engine.activate(t.move(right));
	}
}
