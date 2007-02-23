/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.ast;

import java.util.List;

import orc.runtime.nodes.Node;
import orc.runtime.nodes.Param;



/**
 * Abstract syntax for a use of a defined name.
 * @author mbickford
 */
public class Def extends OrcProcess {
	String name;
	
	public Def(String name) {
		this.name = name;
	}
	
	/** 
	 * A Def is resolved.
	 */
	public OrcProcess resolveNames(List<String> bound, List<String> vals){
		return this;
	}
	
	/** 
	 * When used as a parameter, creates a def node to look up the value.
	 * @see orc.ast.OrcProcess#asParam()
	 */
	public Param asParam() {
		return new orc.runtime.nodes.Def(name);
	}
	
	/** 
	 * A def alone used as a process should either return a closure, or
	 * should be called with no arguments. The Eval node will do that.
	 */
	public Node compile(Node output,List<orc.ast.Definition> defs) {
		return new orc.runtime.nodes.Eval(name, output); 
	}
	
	public boolean isDef(List<orc.ast.Definition> defs) {
		if (defs == null)
			return false;
		for ( orc.ast.Definition def: defs )
			if (def.name.equals(this.name))
				return true;
		return false;
	}
	public  orc.ast.Definition Lookup(List<orc.ast.Definition> defs) {
		for ( orc.ast.Definition def: defs )
			if (def.name.equals(this.name))
				return def;
		return null;
	}
	
	public boolean isSimple() {
		return true; 
	}

	public String toString() {
		return name;
	}
	
	public String Name() {
		return name; 
	}
}
