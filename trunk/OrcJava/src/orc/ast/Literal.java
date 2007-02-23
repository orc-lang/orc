/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.ast;

import java.util.List;

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
	 * A literal is already resolved.
	 */
	public OrcProcess resolveNames(List<String> bound, List<String> vals){
		return this;
	}
	
	/**
	 * Creates a literal node 
	 * @see orc.ast.OrcProcess#compile(orc.runtime.nodes.Node)
	 */
	public Node compile(Node output,List<orc.ast.Definition> defs) {
		return new orc.runtime.nodes.Literal(value, output);
	}

	/** 
	 * When used as a parameter, the literal just outputs its value.
	 * @see orc.ast.OrcProcess#asParam()
	 */
	public Param asParam() {
		return new orc.runtime.nodes.Literal(value, null);
	}
	public boolean isSimple() {
		return true; 
	}
	public boolean isValue() {
		return true; 
	}
}
