package orc.ast.oil;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;
import orc.env.Env;
import orc.error.compiletime.typing.TypeException;
import orc.type.ArrowType;
import orc.type.Type;

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
	public ArrowType type;
	
	public Def(int arity, Expr body) {
		this(arity, body, null);
	}
	
	public Def(int arity, Expr body, ArrowType type) {
		this.arity = arity;
		this.body = body;
		this.type = type;
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
	
	// Analagous to sites, we can ask for the type of a definition,
	// but here it is not a static method.
	public ArrowType type() {
		return type;
	}
	
	/* Make sure this definition checks against its stated type */
	public void typecheck(Env<Type> ctx) throws TypeException {
		
		Env<Type> bodyctx = ctx;
		
		for (Type t : type.argTypes) {
			bodyctx = bodyctx.add(t);
		}

		body.typecheck(type.resultType, bodyctx);
	}
	
}
