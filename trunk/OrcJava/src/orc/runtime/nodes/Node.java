/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.nodes;

import orc.runtime.OrcEngine;
import orc.runtime.Token;

/**
 * Abstract base class for compile nodes
 * @author wcook
 */
public abstract class Node {
	/**
	 * The process method is the fundamental opreation in the execution engine.
	 * It is called to perform the action of the node given a token and
	 * the execution engine.
	 * @param t      input token being processed 
	 * @param engine used to activate the next token
	 */
	public abstract void process(Token t, OrcEngine engine);
}
