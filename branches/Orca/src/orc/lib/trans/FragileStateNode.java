//
// FragileStateNode.java -- Java class FragileStateNode
// Project Orca
//
// $Id$
//
// Created by dkitchin on Dec 16, 2009.
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.trans;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import orc.lib.trans.FrozenCall;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.Site;
import orc.runtime.transaction.Transaction;

/**
 * 
 *
 * @author dkitchin
 */
public class FragileStateNode<T extends Versioned<T>> {

	public T state;
	public HashMap<Transaction, FragileStateNode<T>> children;
	public FragileStateNode<T> parent = null;
	public LinkedList<FrozenCall> frozenCalls;
	public Transaction frozenFor = null;
	
	public FragileStateNode(T state) {
		this.state = state;
		this.children = new HashMap<Transaction, FragileStateNode<T>>();
		this.frozenCalls = new LinkedList<FrozenCall>();
	}

	
	public FragileStateNode(
			T state,
			HashMap<Transaction, FragileStateNode<T>> children,
			FragileStateNode<T> parent,
			LinkedList<FrozenCall> frozenCalls,
			Transaction frozenFor) {
		this.state = state;
		this.children = children;
		this.parent = parent;
		this.frozenCalls = frozenCalls;
		this.frozenFor = frozenFor;
	}
	
	public void mergeFrom(FragileStateNode<T> other) {
		this.state.merge(other.state);
	}
	
	public void fragileChange() {
		// If this state is changed, kill all of its child transactions.
		
		// Fragile conflict resolution.
		// When a transaction commits, all siblings at the site are victims
		
		// This copy loop avoids a concurrent modification exception
		List<Transaction> victims = new LinkedList<Transaction>();
		victims.addAll(children.keySet());
		children.clear();
		for (Transaction victim : victims) {
			victim.abort();
		}
	}
	
	public FragileStateNode<T> addDescendant(Transaction trans) {
		FragileStateNode<T> descendant = new FragileStateNode<T>(state.branch());
		descendant.parent = this;
		children.put(trans, descendant);
		
		return descendant;
	}

	
	public void freeze(Transaction trans) {
		if (frozenFor == null) {
			frozenFor = trans;
		}
		else {
			/* A transaction readying to commit on top of another commit just gets booted.
			 * We assume that the current readying transaction will succeed, at which point
			 * this transaction will get aborted anyway.
			 */
			trans.abort();
		}
	}
	
	/**
	 * @return
	 */
	public boolean isFrozen() {
		return frozenFor != null;
	}

	/**
	 * @param transactionalSite
	 * @param args
	 * @param reader
	 * @param transaction
	 */
	public void addFrozenCall(Site site,
			Args args, Token reader, Transaction transaction) {
		frozenCalls.add(new FrozenCall(args, reader, transaction, site));
		
	}
	
	public void unfreeze() {
		
		// This copy loop avoids a concurrent modification exception
		List<FrozenCall> calls = new LinkedList<FrozenCall>();
		calls.addAll(frozenCalls);
		frozenCalls.clear();
		frozenFor = null;
		for (FrozenCall c : calls) {
			c.unfreeze();
		}
	}
	
}
