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
		assert(!closed);
		inc(); 
		containedTokens.add(t);
	}
	
	public final synchronized void add(Region r) { 
		assert(!closed);
		inc(); 
		containedRegions.add(r);
	}
	
	public synchronized void remove(Token closer) { 
		if (closed) return;
		dec(closer); 
		containedTokens.remove(closer);
	}
	
	public synchronized void remove(Region r, Token closer) { 
		if (closed) return;
		dec(closer); 
		containedRegions.remove(r);
	}

	private void inc() {
		inhabitants++;
	}
	
	private void dec(Token closer) {
		inhabitants--;
		if (inhabitants <= 0) { close(closer); }
	}
	
	public final synchronized void close(Token closer) {
		if (closed) return;
		closed = true;
		reallyClose(closer);
	}
	
	protected abstract void reallyClose(Token closer);
	
	/**
	 * Used when tracing, to both close the region and trace the "choking" of
	 * all tokens within the region. If you're not tracing, it's more efficient
	 * to call {@link #close(Token)}.
	 * 
	 * @param store The {@link StoreTrace} which triggered the closing.
	 */
	public final synchronized void close(StoreTrace store, Token closer) {
		if (closed) return;
		closed = true;
		reallyClose(closer);
		// if the region was already closed,
		// it won't contain anything and so
		// none of this will run
		for (Region r : containedRegions) {
			r.close(store, closer);
		}
		containedRegions.clear();
		for (Token t : containedTokens) {
			t.getTracer().choke(store);
			// don't kill the tokens; they will
			// die on their own as they are processed
		}
		containedTokens.clear();
	}
	
	// for checkpointing
	public void putContainedTokens(Set<Token> acc) {

		acc.addAll(containedTokens);
		for (Region r : containedRegions) {
			r.putContainedTokens(acc);
		}
	}
}
