//
// FragileStateTree.java -- Java class FragileStateTree
// Project Orca
//
// $Id$
//
// Created by dkitchin on Dec 15, 2009.
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

import orc.error.runtime.TokenException;
import orc.lib.trans.FragileTransactionalBuffer.BufferInstance;
import orc.lib.trans.FragileTransactionalBuffer.BufferState;
import orc.lib.trans.FragileTransactionalBuffer.FrozenCall;
import orc.lib.trans.FragileTransactionalRef.RefInstance.RefState;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.Site;
import orc.runtime.transaction.Cohort;
import orc.runtime.transaction.Transaction;

/**
 * 
 *
 * @author dkitchin
 */
public class FragileStateTree<T extends Versioned<T>> implements Cohort {
		
		FragileStateNode<T> root;
		
		public FragileStateTree(T rootState) {
			root = new FragileStateNode<T>(rootState);
		}
	
		
		
		protected static List<Transaction> transactionToPath(Transaction t) {
			List<Transaction> path = new LinkedList<Transaction>();
			while (t != null) {
				path.add(0, t);
				t = t.parent;
			}
			return path;
		}
		
		
		
		public T getRoot() {
			return root.state;
		}
		
		
		
		public FragileStateNode<T> locate(Transaction transaction) {
			
			FragileStateNode<T> target = root;
			
			for (Transaction t : transactionToPath(transaction)) {
				if (!target.children.containsKey(t)) {
					target.addDescendant(t);
				}
				target = target.children.get(t);
			}
			
			return target;
		}
		
		public T retrieve(Transaction transaction, Site site, Args args, Token caller) {
			
			/* Find the state node associated with this transaction */
			FragileStateNode<T> node = locate(transaction);
			
			/* If the node is frozen, queue this call and return */
			if (node.isFrozen()) {
				node.addFrozenCall(site, args, caller, transaction);
				return null;
			}
			
			/* This is an effectful operation, so abort all subtransactions. */
			node.fragileChange();
			
			// Register this tree as a cohort if this call is transactional
			if (transaction != null) {
				transaction.addCohort(this);
			}
			
			return node.state;
		}
		
		
		
		

		public void confirm(Token t, Transaction trans) {
			FragileStateNode<T> source = locate(trans);
			FragileStateNode<T> target = source.parent;
			target.mergeFrom(source);
			target.children.remove(trans);

			/* This is an effectful operation, so abort all siblings. */
			target.fragileChange();

			target.unfreeze();
			t.die();
		}

		/* 
		 * Freeze the target of the commit (the parent state version)
		 */
		public void ready(Token t, Transaction trans) {
			FragileStateNode<T> target = locate(trans);
			target.parent.freeze(trans);
			t.die();
		}

		/* 
		 * Aborts are cheap; just remove this state version from the tree.
		 */
		public void rollback(Token t, Transaction trans) {
			FragileStateNode<T> victim = locate(trans);
			victim.parent.children.remove(trans); // TODO: Make this threadsafe
			t.die();
		}


	
}
