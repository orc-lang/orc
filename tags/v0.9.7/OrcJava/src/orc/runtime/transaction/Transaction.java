
package orc.runtime.transaction;

import java.util.Set;
import java.util.TreeSet;

import orc.error.runtime.TokenLimitReachedError;
import orc.runtime.Token;
import orc.runtime.nodes.Node;
import orc.runtime.nodes.Silent;
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
	
	public synchronized void abort() {
		
		// Ensure that abort is idempotent
		if (!aborted) {
			aborted = true;	
			rollback();
			cell.close();
		}
	}

	public Set<Cohort> getCohorts() {
		return cohorts;
	}

	public synchronized void addCohort(Cohort c) {
		cohorts.add(c);
	}

	/**
	 * A TransRegion will call this method when
	 * there are no more live tokens inside the
	 * transaction.
	 * 
	 * If the transaction has been aborted, this call is ignored.
	 */
	public synchronized void prepareCommit() {
		if (!aborted) {
			ready();
		}
	}
	
	/**
	 * A ReadyRegion will call this method when
	 * there are no more tokens waiting for cohort responses.
	 * 
	 * If the transaction has been aborted, this call is ignored.
	 */
	public synchronized void verifyCommit() {
		if (!aborted) {
			confirm();
		}
	}
	
	
	private void ready() {
		
		try {
		
			ReadyRegion r = new ReadyRegion(initial.getRegion(), this);
			
			for (Cohort c : cohorts) {
				c.ready(initial.fork().setRegion(r).move(Silent.ONLY), this);
			}
		
		} catch (TokenLimitReachedError e) {
			// TODO Make this robust to token limits
			e.printStackTrace();
		}
	}
	
	private void confirm() {
	 
		try {
			
			SemiRegion r = new SemiRegion(initial.getRegion(), initial);
		
			for (Cohort c : cohorts) {
				c.confirm(initial.fork().setRegion(r).move(Silent.ONLY), this);
			}
		
			if (cell.isBound()) {
				/*
				 * If the transaction finished because of a publication, it
				 * commits to that published value.
				 */
				Object v = cell.peekValue();
				initial.setResult(v).move(next);
			}
			else {
				/*
				 * If the transaction finished because all of its tokens died
				 * without publishing, it commits to halting.
				 */
				initial.move(Silent.ONLY);
			}
		
		} catch (TokenLimitReachedError e) {
			// TODO Make this robust to token limits
			e.printStackTrace();
		}
		
	}
	
	private void rollback() {
		
		try {

			SemiRegion r = new SemiRegion(initial.getRegion(), initial);

			for (Cohort c : cohorts) {
				c.rollback(initial.fork().setRegion(r).move(Silent.ONLY), this);
			}

		} catch (TokenLimitReachedError e) {
			// TODO Make this robust to token limits
			e.printStackTrace();
		}
		
	}

}