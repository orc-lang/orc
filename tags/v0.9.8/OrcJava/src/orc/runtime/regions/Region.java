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
public abstract class Region {
	private int inhabitants = 0;
	protected boolean closed = false;
	private Set<Token> containedTokens = new HashSet<Token>();
	private Set<Region> containedRegions = new HashSet<Region>();
	
	public Region() {}
	
	public final synchronized void add(Token t) { 
		if (closed) return;
		inc(); 
		containedTokens.add(t);
	}
	
	public final synchronized void add(Region r) { 
		assert(!closed);
		inc(); 
		containedRegions.add(r);
	}
	
	public final synchronized void remove(Token closer) { 
		if (closed) return;
		dec(); 
		containedTokens.remove(closer);
	}
	
	public final synchronized void remove(Region r) { 
		if (closed) return;
		dec(); 
		containedRegions.remove(r);
	}

	private void inc() {
		inhabitants++;
	}
	
	private void dec() {
		inhabitants--;
		if (inhabitants <= 0) { close(); }
	}
	
	public final synchronized void close() {
		if (closed) return;
		closed = true;
		onClose();
	}
	
	protected abstract void onClose();
	
	// for checkpointing and tracing
	public final synchronized void putContainedTokens(Set<Token> acc) {

		acc.addAll(containedTokens);
		for (Region r : containedRegions) {
			r.putContainedTokens(acc);
		}
	}
}
