package orc.ast.oil;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;

/**
 * 
 * A unit of syntax that encapsulates an expression definition. 
 * 
 * Groups of mutually recursive definitions are scoped in the simplified abstract syntax tree by a Def.
 * 
 * @author dkitchin
 *
 */

public class Def {

	public int arity;
	public Expr body;
	
	public Def(int arity, Expr body)
	{
		this.arity = arity;
		this.body = body;
	}	
	
	public orc.runtime.nodes.Def compile() {
		
		orc.runtime.nodes.Node newbody = body.compile(new orc.runtime.nodes.Return());
		
		return new orc.runtime.nodes.Def(arity, newbody);
	}
	
	public void addVars(Set<Integer> indices, int depth) {
		body.addIndices(indices, depth + arity);
	}
	
	public String toString() {
		
		String args = "";
		for(int i = 0; i < arity; i++) 
			args += "."; 
		return "(def " + args + " " + body.toString() + ")";
	}
}
