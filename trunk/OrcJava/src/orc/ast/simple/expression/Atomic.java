package orc.ast.simple.expression;

import java.util.Set;

import orc.ast.oil.expression.Parallel;
import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.NamedVariable;
import orc.ast.simple.argument.Variable;
import orc.env.Env;
import orc.error.compiletime.CompilationException;

public class Atomic extends Expression {

	Expression body;
	
	public Atomic(Expression body)
	{
		this.body = body;
	}
	
	@Override
	public Expression subst(Argument a, NamedVariable x) 
	{
		return new Atomic(body.subst(a,x));
	}

	@Override
	public Set<Variable> vars() {
		return body.vars();
	}

	@Override
	public orc.ast.oil.expression.Expression convert(Env<Variable> vars, Env<String> typevars) throws CompilationException {
		return new orc.ast.oil.expression.Atomic(body.convert(vars, typevars));
	}

}
