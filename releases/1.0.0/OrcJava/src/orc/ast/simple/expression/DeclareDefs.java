package orc.ast.simple.expression;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.FreeVariable;
import orc.ast.simple.argument.Variable;
import orc.ast.simple.type.FreeTypeVariable;
import orc.ast.simple.type.Type;
import orc.ast.simple.type.TypeVariable;
import orc.env.Env;
import orc.error.compiletime.CompilationException;
import orc.runtime.nodes.Node;

public class DeclareDefs extends Expression {

	public List<Def> defs;
	public Expression body;
	
	public DeclareDefs(List<Def> defs, Expression body)
	{
		this.defs = defs;
		this.body = body;
	}
	
	@Override
	public Expression subst(Argument a, FreeVariable x) {
		return new DeclareDefs(Def.substAll(defs, a, x), body.subst(a,x));
	}
	
	/* (non-Javadoc)
	 * @see orc.ast.simple.expression.Expression#subst(orc.ast.simple.type.Type, orc.ast.simple.type.FreeTypeVariable)
	 */
	@Override
	public Expression subst(Type T, FreeTypeVariable X) {
		return new DeclareDefs(Def.substAll(defs, T, X), body.subst(T,X));
	}

	@Override
	public Set<Variable> vars() {
		Set<Variable> freeset = body.vars();
		
		// Standard notion of free vars
		for (Def d : defs)
		{
			freeset.addAll(d.vars());
		}
		
		// Enforce visibility of mutual recursion
		for (Def d : defs)
		{
			freeset.remove(d.name);
		}
		
		return freeset;
	}

	@Override
	public orc.ast.oil.expression.Expression convert(Env<Variable> vars, Env<TypeVariable> typevars) throws CompilationException {
		
		List<Variable> names = new ArrayList<Variable>();
		for (Def d : defs) {
			names.add(d.name);
		}
		Env<Variable> newvars = vars.extendAll(names);
		
		return new orc.ast.oil.expression.DeclareDefs(Def.convertAll(defs, newvars, typevars), 
													  body.convert(newvars, typevars));
	}
	
	public String toString() {
		String repn = "(defs  ";
		for (Def d : defs) {
			repn += "\n  " + d.toString();
		}
		repn += "\n)\n" + body.toString();
		return repn;
	}
}
