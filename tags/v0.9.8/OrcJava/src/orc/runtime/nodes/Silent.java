package orc.runtime.nodes;

import orc.runtime.Token;

/**
 * 
 * The silent node. Tokens which move here are never reactivated or put
 * into another data structure, so they are effectively destroyed, and
 * collected by the Java garbage collector.
 * 
 * @author dkitchin
 *
 */
public class Silent extends Node {

	private static final long serialVersionUID = 1L;

	public static final Silent ONLY = new Silent();
	
	@Override
	public void process(Token t) {
		t.die();
	}

	public boolean isTerminal() { return true; }
}
