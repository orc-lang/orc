/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.nodes;

import orc.error.runtime.TokenLimitReachedError;
import orc.runtime.Token;

/**
 * A compile node that performs a fork to run two subnodes.
 * @author wcook
 */
public class Fork extends Node {
	private static final long serialVersionUID = 1L;
	public Node left;
	public Node right;
	public Fork(Node left, Node right) {
		this.left = left;
		this.right = right;
	}

	public void process(Token t) {
		/*
		if (engine.debugMode)
			engine.debug("Fork", t);
		*/
		Token forked;
		try {
			forked = t.fork();
		} catch (TokenLimitReachedError e) {
			t.error(e);
			return;
		}
		t.move(left).activate();
		forked.move(right).activate();
	}
}
