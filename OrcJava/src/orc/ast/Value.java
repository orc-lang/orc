/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.ast;

import java.util.*;

import orc.runtime.nodes.Node;
import orc.runtime.nodes.Param;


/**
 * Abstract syntax for a use of a variable that is known to be bound to a value.
 * @author mbickford
 */
public class Value extends OrcProcess {
	String var;

	public Value(String var) {
		this.var = var;
	}

	/** 
	 * When used as a parameter, creates a variable node to look up the value.
	 * Note that we used the syntactic information to resolve a name to Value
	 * and now compliation is treating it the same as Variable, so we are losing 
	 * some information. The distinction between Variable and Value was only used to
	 * translate expressions with nested calls.
	 * @see orc.ast.OrcProcess#asParam()
	 */
	public Param asParam() {
		return new orc.runtime.nodes.Variable(var);
	}
	
	/** 
	 * A value variable is already resolved.
	 */
	public OrcProcess resolveNames(List<String> bound, List<String> vals){
		return this;
	}
	
	/** 
	 * A value v used as a process will be compiled as if it were let(v).
	 */
	public Node compile(Node output,List<orc.ast.Definition> defs) {
       //	compile it as let(this)
		Param p = asParam();
		List<Param> param = new ArrayList<Param>();
		param.add(p);
		return new orc.runtime.nodes.Call(new String("let"),param, output,null);
	}
	
	public boolean isSimple() {
		return true; 
	}
	public String toString() {
		return var;
	}
	
	public boolean isValue() {
		return true; 
	}
	public String Name() {
		return var; 
	}
}
