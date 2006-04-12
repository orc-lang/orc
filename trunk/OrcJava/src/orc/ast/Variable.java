/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.ast;

import orc.runtime.nodes.Node;
import orc.runtime.nodes.Param;

/**
 * Abstrac syntax for variables
 * @author wcook
 */
public class Variable extends OrcProcess {
	String var;

	public Variable(String var) {
		this.var = var;
	}

	/** 
	 * When used as a parameter, creates a variable node to look up the value.
	 * @see orc.ast.OrcProcess#asParam()
	 */
	public Param asParam() {
		return new orc.runtime.nodes.Variable(var);
	}

	/** 
	 * Cannot be used as a process. That is "x" alone is not a valid Orc program.1
	 * @see orc.ast.OrcProcess#compile(orc.runtime.nodes.Node)
	 */
	public Node compile(Node output) {
		throw new Error("Only used as a parameter");
	}
	
	public String toString() {
		return var;
	}
}
