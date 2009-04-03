/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.values;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import orc.error.runtime.UncallableValueException;
import orc.runtime.Group;
import orc.runtime.Token;
import orc.runtime.regions.GroupRegion;
import orc.runtime.regions.Region;
import orc.runtime.transaction.Transaction;
import orc.trace.TokenTracer;
import orc.trace.TokenTracer.PullTrace;
import orc.trace.TokenTracer.StoreTrace;
import orc.trace.events.PullEvent;

/**
 * A value container that is also a group. Groups are
 * essential to the evaluation of where clauses: all the 
 * tokens that arise from execution of a where definition
 * are associated with the same group. Once a value is
 * produced for the group, all these tokens are terminated.
 * @author wcook, dkitchin
 */
public final class GroupCell extends Group implements Serializable, Future {

	private static final long serialVersionUID = 1L;
	private Object value;
	private boolean bound = false;
	private List<Token> waitList;
	private Transaction trans;
	private PullTrace pullTrace;
	private StoreTrace storeTrace;
	private Group parent;

	/**
	 * @param pullTrace used to identify the group cell in traces (see {@link TokenTracer#pull()}).
	 */
	public GroupCell(Group parent, PullTrace pullTrace) {
		parent.add(this);
		this.parent = parent;
		this.pullTrace = pullTrace;
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
		StoreTrace store = token.getTracer().store(pullTrace, this.value);
		pullTrace = null;
		bound = true;
		kill();
		if (waitList != null) {
			for (Token t : waitList) {
				if (store != null) {
					t.getTracer().unblock(store);
				}
				t.unsetQuiescent();
				t.activate();
			}
			waitList = null;
		}
		if (store != null) {
			// HACK: for efficiency, if we're not
			// really tracing, we can skip this step
			HashSet<Token> deadTokens = new HashSet<Token>();
			token.getRegion().putContainedTokens(deadTokens);
			for (Token deadToken : deadTokens) {
				deadToken.getTracer().choke(store);
			}
			storeTrace = store;
		}
		// close the region; this is necessary to ensure that
		// anything waiting on that region to proceed can do
		// so immediately even if tokens are pending
		token.getRegion().close();
	}
	
	/**
	 * If a GroupRegion is closed, it closes the associated
	 * cell, even if that cell has not yet been bound. This
	 * is necessary to ensure any tokens waiting on the cell
	 * are killed.
	 * 
	 * <p>This is also called when a transaction is aborted.
	 */
	public void close() {
		alive = false;
		parent.remove(this);
		if (waitList != null) {
			for (Token t : waitList) {
				t.unsetQuiescent();
				t.die();
			}
			waitList = null;
		}
	}
	
	@Override
	public void onKill() {
		parent.remove(this);
		if (trans != null) {
			// If this cell is supporting a transaction, abort that transaction.
			trans.abort();
			trans = null;
		}
	}

	/**
	 * Add a token to the waiting queue of this group
	 * @param t
	 */
	public void waitForValue(Token t) {
		if (isAlive()) {
			if (waitList == null)
				waitList = new LinkedList<Token>();
			t.getTracer().block(pullTrace);
			waitList.add(t);
			t.setQuiescent();
		} else {
			// A token waiting on a dead group cell will remain silent forever.
			t.die();
		}
	}

	public Object forceArg(Token t) {
		if (bound) {
			t.getTracer().useStored(storeTrace);
			return Value.forceArg(value, t);
		} else {
			waitForValue(t);
			return Value.futureNotReady;
		}
	}
	
	public Callable forceCall(Token t) throws UncallableValueException {
		if (bound) {
			t.getTracer().useStored(storeTrace);
			return Value.forceCall(value, t);
		} else {
			waitForValue(t);
			return Value.futureNotReady;
		}
	}
	
	/* A group cell may be hosting a transaction */
	public Transaction getTransaction() {
		return trans;
	}
	
	public void setTransaction(Transaction trans) {
		this.trans = trans; 
	}
	
	/* 
	 * Peek at the bound value of this cell.
	 * If the cell is still unbound, this returns null.
	 */
	public Object peekValue() {
		return value;
	}
	
	/* Check whether this cell is bound. */
	public boolean isBound() {
		return bound;
	}
	
	public String toString() {
		if (!bound) return "_";
		else return value.toString();
	}
}