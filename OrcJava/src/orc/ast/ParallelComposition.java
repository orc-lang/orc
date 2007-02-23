/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.ast;

import java.util.List;

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
	 * To resolve names in a parallel composition, just resolve both.
	 */
	public OrcProcess resolveNames(List<String> bound, List<String> vals){
		return new ParallelComposition(left.resolveNames(bound,vals),
				                       right.resolveNames(bound,vals));
	}

	/**
	 * Creates a Fork node to run both left and right, which
	 * both output to the same node. 
	 * @see orc.ast.OrcProcess#compile(orc.runtime.nodes.Node)
	 */
	public Node compile(Node output,List<orc.ast.Definition> defs) {
		return new Fork(left.compile(output,defs), right.compile(output,defs));
	}

	public String toString() {
		return "{" + left + "\n| " + right + "}";
	}
}
