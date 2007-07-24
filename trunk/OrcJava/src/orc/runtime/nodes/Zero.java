package orc.runtime.nodes;

import orc.runtime.Token;

/**
 * 
 * The silent node. Tokens which move here are never reactivated or put
 * into another data structure, so they are effectively destroyed.
 * 
 * @author dkitchin
 *
 */
public class Zero extends Node {

	private static final long serialVersionUID = 1L;

	@Override
	public void process(Token t) {
		// Do nothing.
	}

}
