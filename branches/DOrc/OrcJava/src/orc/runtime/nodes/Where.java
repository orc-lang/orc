/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.nodes;

import java.rmi.RemoteException;

import orc.ast.simple.arg.Var;
import orc.runtime.Group;
import orc.runtime.Token;
import orc.runtime.regions.GroupRegion;
import orc.runtime.values.GroupCell;

/**
 * A compiled where node
 * @author wcook
 */
public class Where extends Node {
	private static final long serialVersionUID = 1L;
	Node left;
	Var var;
	Node right;

	public Where(Node left, Var var, Node right) {
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
	public void process(Token t) {
		// Create a new group
		Group group = new Group();
		if (!t.getGroup().addChild(group)) {
			// The parent group may have died while
			// we were processing this token.
			t.die();
			return;
		}
		t.copy().bind(var, new GroupCell(group)).move(left).activate();
		t.move(right)
			.setGroup(group)
			.setRegion(new GroupRegion(t.getRegion(), group))
			.activate();
	}
	public String toString() {
		return super.toString() + "(" + var +")";
	}
}
