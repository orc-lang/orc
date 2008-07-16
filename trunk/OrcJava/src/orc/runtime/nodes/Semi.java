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
	 * The input token is activated on the right node,
	 * and a copy is activated on the left node.
	 */
	public void process(Token t) {
		
		SemiRegion region = new SemiRegion(t.getRegion(), t.copy().move(right));
		t.move(left).setRegion(region).activate();
	}

}
