package orc.runtime.regions;

import java.util.HashSet;
import java.util.Set;

import orc.runtime.Token;
import orc.trace.events.StoreEvent;

public abstract class Region {

	int inhabitants = 0;
	Set<Token> containedTokens;
	Set<Region> containedRegions;
	
	public Region() {
		inhabitants = 0;
		containedTokens = new HashSet<Token>();
		containedRegions = new HashSet<Region>();
	}
	
	public void add(Token t) { 
		inc(); 
		containedTokens.add(t);
	}
	
	public void add(Region r) { 
		inc(); 
		containedRegions.add(r);
	}
	
	public void remove(Token t) { 
		dec(); 
		containedTokens.remove(t);
	}
	
	public void remove(Region r) { 
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
	
	public abstract void close();
	
	/**
	 * Used when tracing, to both close the region and trace the "choking" of
	 * all tokens within the region. If you're not tracing, it's more efficient
	 * to call {@link #close()}.
	 * 
	 * @param store The {@link StoreEvent} which triggered the closing.
	 */
	public void close(StoreEvent store) {
		close();
		// if the region was already closed,
		// it won't contain anything and so
		// none of this will run
		for (Region r : containedRegions) {
			r.close(store);
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
