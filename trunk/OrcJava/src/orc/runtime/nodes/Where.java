/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.nodes;

import orc.runtime.OrcEngine;
import orc.runtime.Token;
import orc.runtime.values.GroupCell;

/**
 * A compiled where node
 * @author wcook
 */
public class Where extends Node {
	Node left;
	String var;
	Node right;

	public Where(Node left, String var, Node right) {
		this.left = left;
		this.var = var;
		this.right = right;
	}

	/**
	 * Executing a where node creates a new group within the current group.
	 * The input token is copied and the variable is 
	 * associated with this group cell for execution of the 
	 * left side of the where. The token is then moved to the
	 * right side and it is associated with the new group.
	 * TODO: this could be expressed slightly better by adding a create group
	 * call to a token.
	 * @see orc.runtime.nodes.Node#process(orc.runtime.Token, orc.runtime.OrcEngine)
	 */
	public void process(Token t, OrcEngine engine) {
		if (engine.debugMode)
			engine.debug("Where " + var, t);

		GroupCell cell = t.getGroup().createCell();
		engine.activate(t.copy().bind(var, cell).move(left));
		engine.activate(t.move(right).setGroup(cell));
	}
}
