package orc.ast.simple.expression;

import java.util.Set;
import java.util.HashSet;

import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.NamedVariable;
import orc.ast.simple.argument.Variable;
import orc.env.Env;
import orc.error.compiletime.CompilationException;

public class Throw extends Expression {

	Expression exception;
	
	public Throw(Expression exception){
		this.exception = exception;
	}

	public Expression subst(Argument a, NamedVariable x) 
	{
		return new Throw(exception.subst(a,x));
	}
	
	public Set<Variable> vars(){
		Set s =  new HashSet<Variable>();
		s.add(exception.vars());
		return s;
	}
	
	public orc.ast.oil.expression.Expression convert(Env<Variable> vars, Env<String> typevars) throws CompilationException {
		orc.ast.oil.expression.Expression e = exception.convert(vars, typevars);
		return new orc.ast.oil.expression.Throw(e);
	}
}
