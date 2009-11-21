package orc.runtime.regions;

import java.util.Set;

import orc.runtime.Token;

public final class MultiRegion extends SubRegion {
	
	Set<Token> ts;
	
	/* Create a new group region with the given parent and coupled group cell */
	public MultiRegion(Region parent, Set<Token> ts) {
		super(parent);
		this.ts = ts;
	}
	
	protected void onClose() {
		for (Token t : ts) {
			t.unsetQuiescent();
			t.activate();
		}
		ts.clear();
		super.onClose();
	}

	public void cancel() {
		for (Token t : ts) {
			t.unsetQuiescent();
			t.die();
		}
		ts.clear();
	}
	
}
