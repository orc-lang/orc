package orc.runtime.regions;

import orc.runtime.Token;

public final class SemiRegion extends SubRegion {
	Token t;
	
	/* Create a new group region with the given parent and coupled group cell */
	public SemiRegion(Region parent, Token t) {
		super(parent);
		this.t = t;
	}
	
	protected void onClose() {
		if (t != null) {
			t.unsetQuiescent();
			t.activate();
		}
		super.onClose();
	}

	public void cancel() {
		if (t != null) {
			t.unsetQuiescent();
			t.die();
			t = null;
		}
	}
}
