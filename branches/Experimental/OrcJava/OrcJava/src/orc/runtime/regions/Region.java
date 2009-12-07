package orc.runtime.regions;

import java.util.HashSet;
import java.util.Set;

import orc.runtime.Token;
import orc.trace.TokenTracer.StoreTrace;

/**
 * Regions are used to track when some (sub-)computation terminates
 * and/or becomes quiescent.
 * 
 * <p>Currently the region methods must be synchronized, because tokens
 * can be killed by independent threads (such as site calls in progress),
 * triggering an update on the region. Maybe we should have a separate
 * queue deal with dead tokens so this isn't necessary.
 */
public class Region {
	private int inhabitants = 0;
	private int active = 0;
	private boolean isClosed = false;
	private boolean isActive = false;
	private Set<Token> containedTokens = new HashSet<Token>();
	private Set<Region> containedRegions = new HashSet<Region>();
	
	public Region() {}
	
	/** Add an active token. */
	public final synchronized void add(Token t) { 
		if (isClosed) return;
		inc(); 
		containedTokens.add(t);
		addActive();
	}
	
	/** Add an inactive region. */
	public final synchronized void add(Region r) { 
		assert(!isClosed);
		inc(); 
		containedRegions.add(r);
	}
	
	/** Remove an active token. */
	public final synchronized void remove(Token closer) { 
		if (isClosed) return;
		containedTokens.remove(closer);
		dec(); 
		// possibly close before becoming possibly inactive;
		// we'll check if we're closed so we
		// don't become inactive twice
		removeActive();
	}
	
	/** Remove an inactive region. */
	public final synchronized void remove(Region r) { 
		if (isClosed) return;
		containedRegions.remove(r);
		dec(); 
	}

	private void inc() {
		inhabitants++;
	}
	
	private void dec() {
		inhabitants--;
		if (inhabitants <= 0) { close(); }
	}
	
	/**
	 * Close the region. This may either
	 * be called indirectly, when the last token
	 * leaves the region, or directly, when
	 * a group cell terminates the corresponding region.
	 * This may be safely called multiple times.
	 */
	public final synchronized void close() {
		// we may be closed twice if a group
		// cell closes the region just after the last
		// token leaves it; so check to be safe
		if (isClosed) return;
		isClosed = true;
		// do this before deactivating
		// in case it makes something active
		onClose();
		if (isActive) deactivate();
	}
	
	/** Override this in subclasses to handle the closing of the region. */
	protected void onClose() {}
	
	// for checkpointing and tracing
	public final synchronized void putContainedTokens(Set<Token> acc) {

		acc.addAll(containedTokens);
		for (Region r : containedRegions) {
			r.putContainedTokens(acc);
		}
	}
	
	/** Called when this region becomes quiescent. */
	protected void deactivate() {
		assert(isActive);
		isActive = false;
	}
	
	/** Called when this region becomes not quiescent. */
	protected void activate() {
		isActive = true;
	}
	
	/**
	 * Called when this region might become quiescent.
	 * This should check if the region is really quiescent,
	 * and call {@link #deactivate()} if so. This will never
	 * be called if the region has already closed.
	 */
	protected void maybeDeactivate() {
		deactivate();
	}
	
	/**
	 * Add an active token.
	 * Tokens are active by default so you should
	 * only call this for a token if you previously called {@link #removeActive()}.
	 */
	public final synchronized void addActive() {
		if (!isActive && !isClosed) activate();
		++active;
	}

	/**
	 * Remove an active token.
	 * Tokens are active by default.
	 */
	public synchronized void removeActive() {
		assert(active > 0);
		--active;
		if (active == 0 && !isClosed) maybeDeactivate();
	}
}
