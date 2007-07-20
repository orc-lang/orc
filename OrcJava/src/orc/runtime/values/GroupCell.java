/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.values;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.List;

import orc.runtime.OrcEngine;
import orc.runtime.Token;

/**
 * A value container that is also a group. Groups are
 * essential to the evaluation of where clauses: all the 
 * tokens that arise from execution of a where definition
 * are associated with the same group. Once a value is
 * produced for the group, all these tokens are terminated.
 * @author wcook, dkitchin
 */
public class GroupCell implements Serializable, Future {

	private static final long serialVersionUID = 1L;
	Value value;
	boolean bound;
	boolean alive;
	List<Token> waitList;
	List<GroupCell> children;
	//int calls;

	public GroupCell() {
		bound = false;
		alive = true;
		//calls = 0;
	}

	/**
	 * Groups are organized into a tree. In this case a new
	 * subgroup is created and returned.
	 * When we add a new child cell, we can also remove any dead children
	 * so that their memory can be recycled.
	 * @return the new group
	 */
	public GroupCell createCell() {
		GroupCell n = new GroupCell();
		if (children == null)
			children = new LinkedList<GroupCell>();
		for (Iterator<GroupCell> it = children.iterator(); it.hasNext(); )
		        if (!(it.next().alive))
		            it.remove();
		children.add(n);
		return n;
	}

	/**
	 * This call defines the fundamental behavior of groups:
	 * When the value is bound, all subgroups are killed
	 * and all waiting tokens are activated.
	 * Any outstanding calls from the group are subtracted from the total calls for the engine
	 * so that we can exit from orc properly.
	 * @param value 	the value for the group 
	 * @param engine	engine
	 */
	public void setValue(Value value, OrcEngine engine) {
		//int calls_killed;
		this.value = value;
		bound = true;
		//calls_killed = kill();
		kill();
		//engine.addCall(-calls_killed);
		if (waitList != null)
			for (Token t : waitList)
				engine.activate(t);
	}

	/**
	 * Recursively kills all subgroups.
	 * Return the number of calls that were outstanding in the killed group, so that
	 * they can be removed from the total outstanding in the engine.
	 * After the children are killed, set children to null so that the memory used by the 
	 * killed objects can be recycled.
	 */
	//private int kill() {
	private void kill() {
		//int answer = calls;
		alive = false;
		if (children != null)
			for (GroupCell sub : children)
				//answer += sub.kill();
				sub.kill();
		children = null;
		//return answer;
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
			waitList = new LinkedList<Token>();
		waitList.add(t);
	}

	public Value forceArg(Token t) {
		
		if (bound)
		{
			return value.forceArg(t);
		}
		else
		{
			waitForValue(t);
			return null;
		}
	}
	
	public Callable forceCall(Token t) {
		
		if (bound)
		{
			return value.forceCall(t);
		}
		else
		{
			waitForValue(t);
			return null;
		}
	}

}