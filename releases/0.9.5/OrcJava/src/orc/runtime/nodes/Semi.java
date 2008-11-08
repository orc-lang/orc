/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.nodes;

import orc.error.runtime.TokenLimitReachedError;
import orc.runtime.Token;
import orc.runtime.regions.SemiRegion;

/**
 * A compile node that runs its left side to completion, 
 * then initiates the right side if the left side did not
 * publish anything.
 * Implemented using regions.
 * @author dkitchin
 */
public class Semi extends Node {
	private static final long serialVersionUID = 1L;
	public Node left;
	public Node right;
	public Semi(Node left, Node right) {
		this.left = left;
		this.right = right;
	}

	/** 
	 * The token is forked. The left branch is associated with a new region
	 * that starts the right branch when it completes.
	 */
	public void process(Token t) {
		Token forked;
		try {
			forked = t.fork();
		} catch (TokenLimitReachedError e) {
			t.error(e);
			return;
		}
		forked.unsetPending();
		SemiRegion region = new SemiRegion(t.getRegion(), forked.move(right));
		t.move(left).setRegion(region).activate();
	}
	
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
