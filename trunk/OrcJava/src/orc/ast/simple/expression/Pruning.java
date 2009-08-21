package orc.ast.simple.expression;

import java.util.Set;

import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.NamedVariable;
import orc.ast.simple.argument.Variable;
import orc.env.Env;
import orc.error.compiletime.CompilationException;
import orc.runtime.nodes.Node;
import orc.runtime.nodes.Store;

public class Pruning extends Expression {

	Expression left;
	Expression right;
	Variable v;
	
	public Pruning(Expression left, Expression right, Variable v)
	{
		this.left = left;
		this.right = right;
		this.v = v;
	}
	
	@Override
	public Expression subst(Argument a, NamedVariable x) 
	{
		return new Pruning(left.subst(a,x), right.subst(a,x), v);
	}

	public Set<Variable> vars() {
		
		Set<Variable> s = left.vars();
		s.addAll(right.vars());
		s.remove(v);
		return s;
	}

	@Override
	public orc.ast.oil.expression.Expression convert(Env<Variable> vars, Env<String> typevars) throws CompilationException {
		
		Env<Variable> newvars = vars.clone();
		newvars.add(v);
		
		return new orc.ast.oil.expression.Pruning(left.convert(newvars, typevars), right.convert(vars, typevars), v.name);
	}
	
}
