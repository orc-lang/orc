package orc.runtime.regions;

import java.util.HashSet;
import java.util.Set;

import orc.runtime.Token;
import orc.trace.TokenTracer.StoreTrace;

/**
 * Regions are used to track when some (sub-)computation terminates.
 * 
 * <p>Currently the region methods must be synchronized, because tokens
 * can be killed by independent threads (such as site calls in progress),
 * triggering an update on the region. Maybe we should have a separate
 * queue deal with dead tokens so this isn't necessary.
 */
public abstract class SubRegion extends Region {
	protected Region parent;
	
	public SubRegion(Region parent) {
		this.parent = parent;
		this.parent.add(this);
	}
	
	protected void onClose() {
		this.parent.remove(this);
	}
	
	protected void deactivate() {
		super.deactivate();
		parent.removeActive();
	}
	
	protected void activate() {
		super.activate();
		parent.addActive();
	}
	
	public final Region getParent() {
		return parent;
	}
}
