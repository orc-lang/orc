/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.values;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import orc.error.runtime.UncallableValueException;
import orc.runtime.Token;
import orc.runtime.regions.GroupRegion;
import orc.trace.events.PullEvent;
import orc.trace.events.StoreEvent;

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
	Object value;
	boolean bound;
	boolean alive;
	List<Token> waitList;
	List<GroupCell> children;
	GroupRegion region;
	private PullEvent event;

	public GroupCell() {
		bound = false;
		alive = true;
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
	 * @param token 	the token with the result value for the group 
	 */
	public void setValue(Token token) {
		assert(!bound);
		this.value = token.getResult();
		// trace the binding of the future;
		// if this returns null, we avoid
		// calling related trace methods
		StoreEvent store = token.getTracer().store(event, this.value);
		event = null;
		bound = true;
		kill();
		if (waitList != null) {
			for (Token t : waitList) {
				if (store != null) {
					t.getTracer().unblock(store);
				}
				t.activate();
			}
			waitList = null;
		}
		if (store == null) {
			// HACK: for efficiency, if we're not
			// really tracing, we can run a cheaper
			// close() operation
			region.close(token);
		} else {
			region.close(store, token);
			token.getTracer().free(store);
		}
	}

	/**
	 * Recursively kills all subgroups.
	 * After the children are killed, set children to null so that the memory used by the 
	 * killed objects can be recycled.
	 */
	
	private void kill() {
		alive = false;
		if (children != null)
			for (GroupCell sub : children)
				sub.kill();
		children = null;
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
		if (alive) {
			if (waitList == null)
				waitList = new LinkedList<Token>();
			t.getTracer().block(event);
			waitList.add(t);
		} else {
			// A token waiting on a dead group cell will remain silent forever.
			t.die();
		}
	}

	public Object forceArg(Token t) {
		if (bound) {
			return Value.forceArg(value, t);
		} else {
			waitForValue(t);
			return Value.futureNotReady;
		}
	}
	
	public Callable forceCall(Token t) throws UncallableValueException {
		if (bound) {
			return Value.forceCall(value, t);
		} else {
			waitForValue(t);
			return Value.futureNotReady;
		}
	}
	
	/* GroupCell and GroupRegion refer to each other.
	 * If a region is closed, it kills the associated cell,
	 * even if that cell has not yet been bound.
	 */
	public void close() {
		if (alive) {
			kill();
			if (waitList != null) {
				for (Token t : waitList) {
					t.die();
				}
				waitList = null;
			}
		}
	}
	
	/* GroupCell and GroupRegion refer to each other.
	 * If a cell is bound, it closes the associated region,
	 * even if that region still has live tokens.
	 */
	public void setRegion(GroupRegion region) {
		this.region = region;
	}
	
	public void setPullEvent(PullEvent event) {
		this.event = event;
	}
}