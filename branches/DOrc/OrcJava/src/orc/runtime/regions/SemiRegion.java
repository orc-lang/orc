package orc.runtime.regions;

import java.rmi.RemoteException;

import orc.runtime.Token;

public class SemiRegion extends Region {
	private static final long serialVersionUID = 1L;
	Token t;
	Region parent;
	
	/* Create a new group region with the given parent and coupled group cell */
	public SemiRegion(Region parent, Token t) {
		super();
		this.parent = parent;
		parent.grow();
		this.t = t;
	}
	
	protected void onClose() {
		t.activate();
		parent.shrink();
	}
}
