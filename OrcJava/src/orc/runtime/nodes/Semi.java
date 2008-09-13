/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.nodes;

import orc.runtime.Token;
import orc.runtime.regions.SemiRegion;

/**
 * A compile node that runs its left side to completion, then initiates the right side.
 * Implemented using regions.
 * @author dkitchin
 */
public class Semi extends Node {
	private static final long serialVersionUID = 1L;
	Node left;
	Node right;
	public Semi(Node left, Node right) {
		this.left = left;
		this.right = right;
	}

	/** 
	 * The token is forked. The left branch is associated with a new region
	 * that starts the right branch when it completes.
	 */
	public void process(Token t) {
		Token forked = t.fork();
		forked.unsetPending();
		SemiRegion region = new SemiRegion(t.getRegion(), forked.move(right));
		t.move(left).setRegion(region).activate();
	}
}
