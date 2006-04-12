/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.ast;

import orc.runtime.nodes.Fork;
import orc.runtime.nodes.Node;

/**
 * Parallel compisition: left | right
 * @author wcook
 */
public class ParallelComposition extends OrcProcess {

	OrcProcess left;
	OrcProcess right;

	public ParallelComposition(OrcProcess left, OrcProcess right) {
		this.left = left;
		this.right = right;
	}

	/**
	 * Creates a Fork node to run both left and right, which
	 * both output to the same node. 
	 * @see orc.ast.OrcProcess#compile(orc.runtime.nodes.Node)
	 */
	public Node compile(Node output) {
		return new Fork(left.compile(output), right.compile(output));
	}

	public String toString() {
		return "{" + left + "\n| " + right + "}";
	}
}
