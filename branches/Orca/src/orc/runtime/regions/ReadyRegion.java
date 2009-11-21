package orc.runtime.regions;

import orc.runtime.Token;
import orc.runtime.transaction.Transaction;
import orc.runtime.values.GroupCell;
import orc.trace.events.Event;

/**
 * 
 * This region encloses tokens waiting for verification of a transaction
 * commitment from each cohort site.
 * 
 * When the region closes, the transaction commitment is verified.
 * 
 * @author dkitchin
 *
 */
public class ReadyRegion extends Region {

	Region parent;
	Transaction trans;
	
	/* Create a new group region with the given parent and coupled group cell */
	public ReadyRegion(Region parent, Transaction trans) {
		this.parent = parent;
		this.parent.add(this);
		this.trans = trans;
	}
	
	protected void onClose() {
		parent.remove(this);
		trans.verifyCommit();
	}
}
