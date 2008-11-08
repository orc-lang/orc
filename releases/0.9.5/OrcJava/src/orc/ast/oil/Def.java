package orc.ast.oil;

import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

import orc.ast.oil.arg.Var;
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
		// rename free variables in the body
		// so that when we construct closure environments
		// we can omit the non-free variables
		Set<Var> free = freeVars();
		final HashMap<Integer,Integer> map = new HashMap<Integer, Integer>();
		int i = free.size()-1;
		for (Var v : free) map.put(v.index + arity, (i--) + arity);
		RenameVariables.rename(body, new RenameVariables.Renamer() {
			public int rename(int var) {
				if (var < arity) return var;
				return map.get(var);
			}
		});
		
		orc.runtime.nodes.Node newbody = body.compile(new orc.runtime.nodes.Return());
		return new orc.runtime.nodes.Def(arity, newbody, free);
	}
	
	public final Set<Var> freeVars() {
		Set<Integer> indices = new TreeSet<Integer>();
		this.addIndices(indices, 0);
		
		Set<Var> vars = new TreeSet<Var>();
		for (Integer i : indices) {
			vars.add(new Var(i));
		}
		
		return vars;
	}
	
	public void addIndices(Set<Integer> indices, int depth) {
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
		Env<Type> bodyctx = ctx.clone();
		
		for (Type t : type.argTypes) {
			bodyctx.add(t);
		}

		body.typecheck(type.resultType, bodyctx);
	}
	
}
