/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.nodes;

import orc.ast.simple.arg.Var;
import orc.runtime.Token;
import orc.runtime.regions.GroupRegion;
import orc.runtime.values.GroupCell;

/**
 * A compiled pull node
 * @author dkitchin, wcook
 */
public class Subgoal extends Node {
	private static final long serialVersionUID = 1L;
	Node left;
	Node right;

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
		GroupCell cell = t.getGroup().createCell(t.getTracer().pull());
		GroupRegion region = new GroupRegion(t.getRegion(), cell);
		
		Token forked = t.fork(cell, region);
		t.bind(cell).move(left).activate();
		forked.move(right).activate();
	}
}
