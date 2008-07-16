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
	 * The input token is copied and the variable is 
	 * associated with this group cell for execution of the 
	 * left side of the pull. The token is then moved to the
	 * right side and it is associated with the new group.
	 */
	public void process(Token t) {
		GroupCell cell = t.getGroup().createCell();
		GroupRegion region = new GroupRegion(t.getRegion(), cell);
		
		t.copy().bind(cell).move(left).activate();
		t.move(right).setGroup(cell).setRegion(region).activate();
	}
}
