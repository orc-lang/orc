/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.ast;

import orc.runtime.nodes.Node;
import orc.runtime.nodes.Param;

/**
 * Base class for the abstract syntax tree
 * @author wcook
 */
abstract public class OrcProcess {

	/**
	 * Compiles abstrac syntax tree into execution nodes.
	 * Every node is compile relative to an "output" node that represents
	 * the "rest of the program". Thus the tree of compiled nodes is created bottom up.
	 * @param output IMPORTANT: this is the node to which output will be directed
	 * @return A new node
	 */
	public abstract Node compile(Node output);

	public Param asParam() {
		return null; // overriden by parameter types
	}
}
