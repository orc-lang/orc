package orc.runtime.regions;

import orc.runtime.Token;

public class SemiRegion extends Region {

	Region parent;
	Token t;
	
	/* Create a new group region with the given parent and coupled group cell */
	public SemiRegion(Region parent, Token t) {
		this.parent = parent;
		this.t = t;
		this.parent.add(this);
	}
	
	protected void reallyClose(Token closer) {
		if (t != null) {
			t.unsetQuiescent();
			t.activate();
			/* Add all the halt events to the causes of
			 * the right branch.
			 * EXPERIMENTAL.
			 */
			t.getTracer().enterOther(haltEvents);
		} else {
			/* On closing a semi region, the halt events are added to 
			 * the parent region only when the left branch has already 
			 * published a value. If the left side blocks, all the halt 
			 * events are anyway included in the right side's causes.
			 * EXPERIMENTAL
			 */
			parent.addHaltEvents(haltEvents);
		}
		
		haltEvents=null;
		
		parent.remove(this, closer);
	}

	public Region getParent() {
		return parent;
	}

	public void cancel() {
		if (t != null) {
			// setPending so that the token can unsetPending when it
			// dies
			t.unsetQuiescent();
			t.die();
			t = null;
		}
	}
}
