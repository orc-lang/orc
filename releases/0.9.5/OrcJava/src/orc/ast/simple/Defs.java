package orc.ast.simple;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import orc.ast.oil.Def;
import orc.ast.oil.Expr;
import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;
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
	public Expr convert(Env<Var> vars) throws CompilationException {
		
		List<Var> names = new ArrayList<Var>();
		
		for (Definition d : defs) {
			names.add(d.name);
		}
		
		Env<Var> newvars = vars.clone();
		newvars.addAll(names);
		
		List<Def> newdefs = new ArrayList<Def>();
		
		for (Definition d : defs) {
			Def newd = d.convert(newvars);
			newdefs.add(newd);
		}
		
		return new orc.ast.oil.Defs(newdefs, body.convert(newvars));
	}

	
}
