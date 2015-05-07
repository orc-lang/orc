package orc.runtime.regions;

import orc.runtime.Token;
import orc.runtime.nodes.Isolate;
import orc.trace.TokenTracer.StoreTrace;

/**
 * An isolated region. This prevents the parent region
 * from having a link to the tokens in the region, so
 * they will not be killed when the region is killed,
 * and they may be garbage collected.
 * 
 * See {@link Isolate}.
 * @author quark
 */
public class IsolatedRegion extends Region {
	private Region parent;
	private Region dummy;
	
	public IsolatedRegion(Region root, Region parent) {
		this.parent = parent;
		this.dummy = new Execution();
		this.parent.add(dummy);
	}
	
	@Override
	protected void onClose() {
		parent.remove(dummy);
	}

	public Region getParent() {
		return parent;
	}
}
