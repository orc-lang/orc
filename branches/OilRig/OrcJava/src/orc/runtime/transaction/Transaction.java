
package orc.runtime.transaction;

import java.util.Set;
import java.util.TreeSet;

import orc.error.runtime.TokenLimitReachedError;
import orc.runtime.Token;
import orc.runtime.regions.SemiRegion;
import orc.runtime.values.GroupCell;
import orc.runtime.values.Value;

/**
 * 
 * A transaction (a dynamic instance of an atomic section). Transactions
 * form a tree, as atomic sections may be nested. Every token has a field
 * denoting which transaction it is currently participating in; that
 * field may be null.
 * 
 * @author dkitchin
 *
 */

public class Transaction {
	
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