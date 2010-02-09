//
// Transaction.java -- Java class Transaction
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.runtime.transaction;

/**
 * A transaction (a dynamic instance of an atomic section). Transactions
 * form a tree, as atomic sections may be nested. Every token has a field
 * denoting which transaction it is currently participating in; that
 * field may be null.
 * 
 * @author dkitchin
 */
public class Transaction {

	// Stubbed out, to be revived in a future version of Orc.
	
	/*
	public Transaction parent;
	public Node next;
	
	private GroupCell cell;
	private Set<Cohort> cohorts; 
	private Token initial;
	private boolean aborted = false;
	
	public Transaction(Token initial, Node next, GroupCell cell) {
		this.initial = initial;
		this.parent = initial.getTransaction();
		this.cohorts = new TreeSet();
		this.next = next;
		this.cell = cell;
	}
	*/

}
