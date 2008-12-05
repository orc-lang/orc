package orc.runtime.transaction;

import orc.runtime.Token;
import orc.runtime.regions.Region;
import orc.runtime.values.GroupCell;
import orc.trace.events.Event;

public class ReadyRegion extends Region {

	Region parent;
	Transaction trans;
	
	/* Create a new group region with the given parent and coupled group cell */
	public ReadyRegion(Region parent, Transaction trans) {
		this.parent = parent;
		this.parent.add(this);
		this.trans = trans;
	}
	
	protected void reallyClose(Token closer) {
		parent.remove(this, closer);
		trans.verifyCommit();
	}
}
