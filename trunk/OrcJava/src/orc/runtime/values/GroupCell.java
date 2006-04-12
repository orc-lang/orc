/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.values;

import java.util.ArrayList;
import java.util.List;

import orc.runtime.OrcEngine;
import orc.runtime.Token;

/**
 * A value container that is also a group. Groups are
 * essential to the evaluation of where clauses: all the 
 * tokens that arise from execution of a where definition
 * are associated with the same group. Once a value is
 * produced for the group, all these tokens are terminated.
 * @author wcook
 */
public class GroupCell implements Value {

	Object value;
	boolean bound;
	boolean alive;
	List<Token> waitList;
	List<GroupCell> children;

	public GroupCell() {
		bound = false;
		alive = true;
	}

	/** 
	 * A group is unbound as long as no value has been produced
	 * @see orc.runtime.values.Value#asUnboundCell()
	 */
	public GroupCell asUnboundCell() {
		return bound ? null : this;
	}

	/** 
	 * Once the group is bound, its value can be accessed.
	 * @see orc.runtime.values.Value#asBasicValue()
	 */
	public Object asBasicValue() {
		if (!bound)
			throw new Error("Getting value of unbound cell");
		return value;
	}

	/**
	 * Groups are organized into a tree. In this case a new
	 * subgroup is created and returned
	 * @return the new group
	 */
	public GroupCell createCell() {
		GroupCell n = new GroupCell();
		if (children == null)
			children = new ArrayList<GroupCell>();
		children.add(n);
		return n;
	}

	/**
	 * This call defines the fundamental behavior of groups:
	 * When the value is bound, all subgroups are killed
	 * and all waiting tokens are activated.
	 * @param value 	the value for the group 
	 * @param engine	engine
	 */
	public void setValue(Object value, OrcEngine engine) {
		this.value = value;
		bound = true;
		kill();
		if (waitList != null)
			for (Token t : waitList)
				engine.activate(t);
	}

	/**
	 * Recursively kills all subgroups
	 */
	private void kill() {
		alive = false;
		if (children != null)
			for (GroupCell sub : children)
				sub.kill();
	}

	/**
	 * Check if a group has been killed
	 * @return true if the group has not been killed
	 */
	public boolean isAlive() {
		return alive;
	}

	/**
	 * Add a token to the waiting queue of this group
	 * @param t
	 */
	public void waitForValue(Token t) {
		if (waitList == null)
			waitList = new ArrayList<Token>();
		waitList.add(t);
	}

}