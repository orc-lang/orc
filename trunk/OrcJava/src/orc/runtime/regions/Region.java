package orc.runtime.regions;

import java.util.HashSet;
import java.util.Set;

import orc.runtime.Token;

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
	
		// for chcekpointing
		containedTokens.add(t);
	}
	
	public void add(Region r) { 
		inc(); 
		
		// for chcekpointing
		containedRegions.add(r);
	}
	
	public void remove(Token t) { 
		dec(); 
		
		// for chcekpointing
		containedTokens.remove(t);
	}
	
	public void remove(Region r) { 
		dec(); 

		// for chcekpointing
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
	
	// for checkpointing
	public void putContainedTokens(Set<Token> acc) {

		acc.addAll(containedTokens);
		for (Region r : containedRegions) {
			r.putContainedTokens(acc);
		}
	}
}
