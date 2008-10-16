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

	@Override
	public void process(Token t) {
		t.die();
	}

	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
	
	public boolean isTerminal() { return true; }
}
