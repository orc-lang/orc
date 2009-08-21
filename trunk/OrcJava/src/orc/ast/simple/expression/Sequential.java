package orc.ast.simple.expression;

import java.util.Set;

import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.NamedVariable;
import orc.ast.simple.argument.Variable;
import orc.env.Env;
import orc.error.compiletime.CompilationException;
import orc.runtime.nodes.Assign;
import orc.runtime.nodes.Node;

public class Sequential extends Expression {

	Expression left;
	Expression right;
	Variable v;
	
	public Sequential(Expression left, Expression right, Variable v)
	{
		this.left = left;
		this.right = right;
		this.v = v;
	}
	

	@Override
	public Sequential subst(Argument a, NamedVariable x) 
	{
		return new Sequential(left.subst(a,x), right.subst(a,x), v);
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
		
		return new orc.ast.oil.expression.Sequential(left.convert(vars, typevars), right.convert(newvars, typevars), v.name);
	}

}
