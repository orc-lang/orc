//
// GroupCell.java -- Java class GroupCell
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.runtime.values;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import orc.error.runtime.UncallableValueException;
import orc.runtime.Group;
import orc.runtime.Token;
import orc.trace.TokenTracer;
import orc.trace.TokenTracer.PullTrace;
import orc.trace.TokenTracer.StoreTrace;

/**
 * A value container that is also a group. Groups are
 * essential to the evaluation of where clauses: all the 
 * tokens that arise from execution of a where definition
 * are associated with the same group. Once a value is
 * produced for the group, all these tokens are terminated.
 * @author wcook, dkitchin
 */
public final class GroupCell extends Group implements Serializable, Future {
	private Object value;
	private boolean bound = false;
	private List<Token> waitList;
	private final PullTrace pullTrace;
	private StoreTrace storeTrace;
	private final Group parent;

	/**
	 * @param pullTrace used to identify the group cell in traces (see {@link TokenTracer#pull()}).
	 */
	public GroupCell(final Group parent, final PullTrace pullTrace) {
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
	public void setValue(final Token token) {
		assert !bound;
		this.value = token.getResult();
		// trace the binding of the future;
		// if this returns null, we avoid
		// calling related trace methods
		final StoreTrace store = token.getTracer().store(pullTrace, this.value);
		bound = true;
		kill();
		if (waitList != null) {
			for (final Token t : waitList) {
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
			final HashSet<Token> deadTokens = new HashSet<Token>();
			token.getRegion().putContainedTokens(deadTokens);
			for (final Token deadToken : deadTokens) {
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
	 */
	public void close() {
		alive = false;
		parent.remove(this);
		if (waitList != null) {
			for (final Token t : waitList) {
				t.unsetQuiescent();
				t.die();
			}
			waitList = null;
		}
	}

	@Override
	public void onKill() {
		parent.remove(this);
	}

	/**
	 * Add a token to the waiting queue of this group
	 * @param t
	 */
	public void waitForValue(final Token t) {
		if (isAlive()) {
			if (waitList == null) {
				waitList = new LinkedList<Token>();
			}
			waitList.add(t);
			t.setQuiescent();
		} else {
			// A token waiting on a dead group cell will remain silent forever.
			t.die();
		}
	}

	public Object forceArg(final Token t) {
		t.getTracer().block(pullTrace);
		if (bound) {
			t.getTracer().unblock(storeTrace);
			return Value.forceArg(value, t);
		} else {
			waitForValue(t);
			return Value.futureNotReady;
		}
	}

	public Callable forceCall(final Token t) throws UncallableValueException {
		t.getTracer().block(pullTrace);
		if (bound) {
			t.getTracer().unblock(storeTrace);
			return Value.forceCall(value, t);
		} else {
			waitForValue(t);
			return Value.futureNotReady;
		}
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

	@Override
	public String toString() {
		if (!bound) {
			return "_";
		} else {
			return value.toString();
		}
	}
}
