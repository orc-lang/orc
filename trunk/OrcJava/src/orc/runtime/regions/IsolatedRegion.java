package orc.runtime.regions;

import orc.runtime.Token;
import orc.trace.TokenTracer.StoreTrace;

public class IsolatedRegion extends Region {
	private Region root;
	private Region parent;
	
	public IsolatedRegion(Region root, Region parent) {
		this.parent = parent;
		this.root = root;
		this.parent.add(this);
		this.root.add(this);
	}
	
	@Override
	protected void onClose() {
		parent.remove(this);
		root.remove(this);
	}

	public Region getParent() {
		return parent;
	}
}
