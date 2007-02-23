/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.ast;

import java.util.*;

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
	 * A variable is already resolved.
	 */
	public OrcProcess resolveNames(List<String> bound, List<String> vals){
		return this;
	}
	


	/** 
	 * A variable v used as a process has to be compiled to
	 * something that will treat it like let(v) if v is bound
	 * but like a call v() if v is a definition.
	 */
	public Node compile(Node output,List<orc.ast.Definition> defs) {
       	return new orc.runtime.nodes.Eval(var, output); 
		}
	
	public boolean isSimple() {
		return true; 
	}
	public String toString() {
		return var;
	}
	
	public String Name() {
		return var; 
	}
}
