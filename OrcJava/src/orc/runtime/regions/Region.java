package orc.runtime.regions;

import orc.runtime.Token;

public abstract class Region {

	int inhabitants;
	
	public void add(Token t) { inc(); }
	public void add(Region r) { inc(); }
	public void remove(Token t) { dec(); }
	public void remove(Region r) { dec(); }

	private void inc() {
		inhabitants++;
	}
	
	private void dec() {
		inhabitants--;
		if (inhabitants <= 0) { close(); }
	}
	
	public abstract void close();
	
}
