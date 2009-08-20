package orc.ast.simple.expression;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import orc.ast.oil.Def;
import orc.ast.oil.expression.Expr;
import orc.ast.simple.Definition;
import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.NamedVar;
import orc.ast.simple.argument.Var;
import orc.env.Env;
import orc.error.compiletime.CompilationException;
import orc.runtime.nodes.Node;

public class Defs extends Expression {

	public List<Definition> defs;
	public Expression body;
	
	public Defs(List<Definition> defs, Expression body)
	{
		this.defs = defs;
		this.body = body;
	}
	
	@Override
	public Expression subst(Argument a, NamedVar x) {
		
		List<Definition> newdefs = new LinkedList<Definition>();
		for (Definition d : defs)
		{
			newdefs.add(d.subst(a,x));
		}
		
		return new Defs(newdefs, body.subst(a,x));
	}

	@Override
	public Set<Var> vars() {
		Set<Var> freeset = body.vars();
		
		// Standard notion of free vars
		for (Definition d : defs)
		{
			freeset.addAll(d.vars());
		}
		
		// Enforce visibility of mutual recursion
		for (Definition d : defs)
		{
			freeset.remove(d.name);
		}
		
		return freeset;
	}

	@Override
	public Expr convert(Env<Var> vars, Env<String> typevars) throws CompilationException {
		
		List<Var> names = new ArrayList<Var>();
		
		for (Definition d : defs) {
			names.add(d.name);
		}
		
		Env<Var> newvars = vars.clone();
		newvars.addAll(names);
		
		List<Def> newdefs = new ArrayList<Def>();
		
		for (Definition d : defs) {
			Def newd = d.convert(newvars, typevars);
			newdefs.add(newd);
		}
		
		return new orc.ast.oil.expression.Defs(newdefs, body.convert(newvars, typevars));
	}

	
}
