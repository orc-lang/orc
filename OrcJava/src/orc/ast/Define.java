/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.ast;

import java.util.List;

import orc.runtime.nodes.Node;
import orc.runtime.nodes.Return;
import orc.runtime.values.Tuple;

/**
 * Abstract syntax for defintions (which can be nested) with the form
 * <pre>
 *    def name(formals) = body 
 *    rest
 * </pre> 
 * Where rest is the program in which the name is bound
 * @author wcook
 */
public class Define extends OrcProcess {

	String name;
	List<String> formals;
	OrcProcess body;
	OrcProcess rest;

	public Define(String name, List<String> formals, OrcProcess body,
			OrcProcess rest) {
		this.name = name;
		this.formals = formals;
		this.body = body;
		this.rest = rest;
	}

	/**
	 * Compiles the body with output to a return node.
	 * Creates a define node (which will created the binding) and
	 * then invoke the rest of the program.
	 * @see orc.ast.OrcProcess#compile(orc.runtime.nodes.Node)
	 */
	public Node compile(Node output) {
		Node bodyNode = body.compile(new Return());
		Node restNode = rest.compile(output);
		return new orc.runtime.nodes.Define(name, formals, bodyNode, restNode);
	}

	public String toString() {
		return "def " + name + Tuple.format('(', formals, ", ", ')') + " =\n   "
				+ body + "\n" + rest;
	}
}
