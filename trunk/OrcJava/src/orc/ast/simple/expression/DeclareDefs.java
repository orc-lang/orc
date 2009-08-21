package orc.ast.simple.expression;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.NamedVariable;
import orc.ast.simple.argument.Variable;
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
	public Expression subst(Argument a, NamedVariable x) {
		
		List<Def> newdefs = new LinkedList<Def>();
		for (Def d : defs)
		{
			newdefs.add(d.subst(a,x));
		}
		
		return new DeclareDefs(newdefs, body.subst(a,x));
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
	public orc.ast.oil.expression.Expression convert(Env<Variable> vars, Env<String> typevars) throws CompilationException {
		
		List<Variable> names = new ArrayList<Variable>();
		
		for (Def d : defs) {
			names.add(d.name);
		}
		
		Env<Variable> newvars = vars.clone();
		newvars.addAll(names);
		
		List<orc.ast.oil.expression.Def> newdefs = new ArrayList<orc.ast.oil.expression.Def>();
		
		for (Def d : defs) {
			orc.ast.oil.expression.Def newd = d.convert(newvars, typevars);
			newdefs.add(newd);
		}
		
		return new orc.ast.oil.expression.DeclareDefs(newdefs, body.convert(newvars, typevars));
	}

	
}
