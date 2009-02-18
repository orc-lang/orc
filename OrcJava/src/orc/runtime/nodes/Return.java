/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.nodes;

import orc.runtime.Token;

/**
 * Compiled node marking the end of a procedure
 * @author wcook
 */
public class Return extends Node {
	private static final long serialVersionUID = 1L;

	public void process(Token t) {
		t.leaveClosure().activate();
	}
}
