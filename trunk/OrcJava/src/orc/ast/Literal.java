/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.ast;

import orc.runtime.nodes.Node;
import orc.runtime.nodes.Param;

/**
 * Abstract syntax for literals
 * @author wcook
 */
public class Literal extends OrcProcess {
	public Object value;

	public Literal(Object value) {
		this.value = value;
	}

	public String toString() {
		if (value instanceof String)
			return "\"" + value + "\"";
		else
			return value.toString();
	}
	
	/**
	 * Creates a literal node 
	 * @see orc.ast.OrcProcess#compile(orc.runtime.nodes.Node)
	 */
	public Node compile(Node output) {
		return new orc.runtime.nodes.Literal(value, output);
	}

	/** 
	 * When used as a parameter, the literal just outputs its value.
	 * @see orc.ast.OrcProcess#asParam()
	 */
	public Param asParam() {
		return new orc.runtime.nodes.Literal(value, null);
	}
}
