/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.ast;

import orc.runtime.nodes.Assign;
import orc.runtime.nodes.Fork;
import orc.runtime.nodes.Node;

/**
 * Abstract syntax tree for
 * <pre>
 *    left >[!] [var]> right
 * </pre>
 * Both ! and var are optional.
 * If ! is present, then publish is true and the var should be output.
 * @author wcook
 */
public class SequentialComposition extends OrcProcess {
	OrcProcess left;
	String var;
	boolean publish;
	OrcProcess right;

	public SequentialComposition(OrcProcess left, String var,
			boolean publish, OrcProcess right) {
		this.left = left;
		this.var = var;
		this.publish = publish;
		this.right = right;
	}

	/** 
	 * Compile the right side relative to the overall program output.
	 * If the variable is present then create an 
	 * assign node.
	 * If the result should be published, create a fork.
	 * This is because
	 * <pre>
	 *    f >!v> g
	 * </pre>
	 * is equivalent to 
	 * <pre>
	 *    f >v> (let(x) | g)
	 * </pre>
	 * Finally, compile the left side and send its output 
	 * to the newly created node for the right side.
	 * @see orc.ast.OrcProcess#compile(orc.runtime.nodes.Node)
	 */
	public Node compile(Node output) {
		Node node = right.compile(output);
		if (var != null && var.length() > 0)
			node = new Assign(var, node);
		if (publish)
			node = new Fork(node, output);
		return left.compile(node);
	}

	public String toString() {
		return "{" + left + "\n>" + (publish ? "!" : "") + var + "> " + right + "}";
	}
}
