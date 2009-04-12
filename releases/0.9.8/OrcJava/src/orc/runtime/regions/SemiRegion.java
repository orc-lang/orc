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
	
	protected void onClose() {
		if (t != null) {
			t.unsetQuiescent();
			t.activate();
		}
		parent.remove(this);
	}

	public Region getParent() {
		return parent;
	}

	public void cancel() {
		if (t != null) {
			t.unsetQuiescent();
			t.die();
			t = null;
		}
	}
}
