/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.nodes;

import orc.ast.simple.arg.Var;
import orc.error.runtime.TokenLimitReachedError;
import orc.runtime.Token;
import orc.runtime.regions.GroupRegion;
import orc.runtime.values.GroupCell;

/**
 * A compiled pull node
 * @author dkitchin, wcook
 */
public class Subgoal extends Node {
	private static final long serialVersionUID = 1L;
	public Node left;
	public Node right;

	public Subgoal(Node left, Node right) {
		this.left = left;
		this.right = right;
	}

	/**
	 * Executing a subgoal node creates a new group within the current group.
	 * The token is forked, and the fork is associated with a new group cell
	 * and region.
	 */
	public void process(Token t) {
		GroupCell cell = new GroupCell(t.getGroup(), t.getTracer().pull());
		GroupRegion region = new GroupRegion(t.getRegion(), cell);
		
		Token forked;
		try {
			forked = t.fork(cell, region);
		} catch (TokenLimitReachedError e) {
			t.error(e);
			return;
		}
		t.bind(cell).move(left).activate();
		forked.move(right).activate();
	}
}
