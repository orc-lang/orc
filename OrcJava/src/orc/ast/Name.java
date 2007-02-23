/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.ast;

import java.util.List;

import orc.runtime.nodes.Node;


/**
 * Abstrac syntax for names
 * These will be resolved into variables, values, or definitions during compliation
 * @author mbickford
 */
public class Name extends OrcProcess {
	String name;

	public Name(String nm) {
		this.name = nm;
	}

	/** 
	 * Resolve a name to Variable if it is on the list of bound variables,
	 * Value if it is on the list of variables known to be values
	 * and othewise resolve it to Def.
	 *  
	 */
	public OrcProcess resolveNames(List<String> bound, List<String> vals){
		if (bound.contains(name)){
			//System.out.println(name + "= variable");
			return new Variable(name);
		}
		else if (vals.contains(name)){
			//System.out.println(name + "= variable");
			return new Value(name);
		}
		else { //System.out.println(name + "= def");
		     return new Def(name);
		}
		
	}
	/** 
	 * Names should be replaced by either Variable or Def before being compiled.
	 * So it will be an error to invoke the compile method on an unresolved name.
	 * @see orc.ast.OrcProcess#compile(orc.runtime.nodes.Node)
	 */
	public Node compile(Node output,List<orc.ast.Definition> defs) {
		throw new Error("Unresolved name: "+ name);
	}
	
	public String toString() {
		return name;
	}
	
	
//	public String Name() {
//		return name; 
//	}
}
