package orc.runtime.regions;

import orc.runtime.Token;
import orc.runtime.nodes.Isolate;
import orc.trace.TokenTracer.StoreTrace;

/**
 * An isolated region. This does two things:
 * first, it prevents any parent region from linking to
 * tokens in this region, so they may be garbage collected.
 * Second, it records liveness and quiescence in the root region
 * as well as the parent, so the root can't terminate until
 * we do.
 * 
 * See {@link Isolate}.
 * @author quark
 */
public final class IsolatedRegion extends Region {
	private Region parent;
	private Region dummy;
	private Region root;
	
	public IsolatedRegion(Region root, Region parent) {
		this.parent = parent;
		this.root = root;
		this.dummy = new Region();
		this.parent.add(dummy);
		root.add(dummy);
	}
	
	@Override
	protected void onClose() {
		parent.remove(dummy);
		root.remove(dummy);
	}

	public Region getParent() {
		return parent;
	}
	
	@Override
	protected void deactivate() {
		super.deactivate();
		parent.removeActive();
		root.removeActive();
	}
	
	@Override
	protected void activate() {
		super.activate();
		parent.addActive();
		root.addActive();
	}
}
