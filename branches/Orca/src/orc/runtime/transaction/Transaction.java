
package orc.runtime.transaction;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import orc.error.runtime.TokenLimitReachedError;
import orc.runtime.Token;
import orc.runtime.nodes.Node;
import orc.runtime.nodes.Silent;
import orc.runtime.regions.MultiRegion;
import orc.runtime.regions.ReadyRegion;
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
	
	
	/* Running transactions form a tree, since atomic combinators may be nested */
	public Transaction parent;
	public Set<Transaction> children;
	
	/* The node where publication tokens will arrive after a commit */
	public Node next;
	
	/*
	 * All tokens participating in a transaction are members of a group cell.
	 * If the transaction aborts, all of its participants are immediately killed using this cell.
	 * In other words, we do not allow zombie transactions.
	 */
	private GroupCell cell;
	
	/*
	 * A cohort is an interface to a site participating in a transaction.
	 * Each site called within the transaction becomes a cohort in that transaction. 
	 */
	private Set<Cohort> cohorts; 
	
	
	/*
	 * The publications produced within this transaction. All publications are
	 * released simultaneously when the transaction commits.
	 */
	private List<Object> publications;
	
	/*
	 * The token which initiated the transaction. Copies of this token carrying
	 * each value published by the transaction will be forked when the transaction
	 * commits.
	 */
	private Token initial;
	
	/*
	 * Whether this transaction has been aborted. This flag is monotonic.
	 * This lets us ensure that aborting is idempotent.
	 */
	private boolean aborted = false;
	
	
	public Transaction(Token initial, Node next, GroupCell cell) {
		this.initial = initial;
		this.parent = initial.getTransaction();
		this.children = new TreeSet();
		this.cohorts = new TreeSet();
		this.publications = new LinkedList<Object>();
		this.next = next;
		this.cell = cell;
	}
	
	public synchronized void abort() {
		
//		System.out.println("aborted");
		
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

	public synchronized void addChild(Transaction t) {
		children.add(t);
	}
	
	/**
	 * A TransRegion will call this method when
	 * there are no more live tokens inside the
	 * transaction.
	 * 
	 * If the transaction has been aborted, this call is ignored.
	 */
	public synchronized void prepareCommit() {
//		System.out.println("readied");
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
//		System.out.println("verified");
		if (!aborted) {
			confirm();
		}
	}
	
	
	private void ready() {
		
		try {
		
//			System.out.println("Ready checking");
			if (!cohorts.isEmpty()) {
				ReadyRegion r = new ReadyRegion(initial.getRegion(), this);
				
				for (Cohort c : cohorts) {
					c.ready(initial.fork().setRegion(r).move(Silent.ONLY), this);
				}
			}
			else {
//				System.out.println("No cohorts");
				confirm();
			}
		
		} catch (TokenLimitReachedError e) {
			// TODO Make this robust to token limits
			e.printStackTrace();
		}
	}
	
	private void confirm() {
	 
		try {
			/* When the commit is confirmed, release all of the publications */
			
			if (!cohorts.isEmpty()) {
				Set<Token> ts = new TreeSet();
				for (Object v : publications) {
					ts.add(initial.fork().move(next).setResult(v));
				}
				
				MultiRegion r = new MultiRegion(initial.getRegion(), ts);
				
				for (Cohort c : cohorts) {
					c.confirm(initial.fork().setRegion(r).move(Silent.ONLY), this);
				}
			}
			else {
				for (Object v : publications) {
					initial.fork().move(next).resume(v);
				}
				
			}
			
			initial.die();
		
		} catch (TokenLimitReachedError e) {
			// TODO Make this robust to token limits
			e.printStackTrace();
		}
		
	}
	
	private void rollback() {
		
//		System.out.println("rolling back");
		
		try {

			/* When the rollback is complete, retry the transaction from the top. */
			SemiRegion r = new SemiRegion(initial.getRegion(), initial);

			for (Cohort c : cohorts) {
				c.rollback(initial.fork().setRegion(r).move(Silent.ONLY), this);
			}

		} catch (TokenLimitReachedError e) {
			// TODO Make this robust to token limits
			e.printStackTrace();
		}
		
	}

	/**
	 * @param result
	 */
	public void registerPublication(Object result) {
		publications.add(result);
	}

}