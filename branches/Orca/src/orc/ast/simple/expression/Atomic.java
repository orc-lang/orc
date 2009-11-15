package orc.ast.simple.expression;

import java.util.Set;

import orc.ast.oil.expression.Parallel;
import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.FreeVariable;
import orc.ast.simple.argument.Variable;
import orc.ast.simple.type.FreeTypeVariable;
import orc.ast.simple.type.Type;
import orc.ast.simple.type.TypeVariable;
import orc.env.Env;
import orc.error.compiletime.CompilationException;

public class Atomic extends Expression {

	Expression body;
	
	public Atomic(Expression body)
	{
		this.body = body;
	}
	
	@Override
	public Expression subst(Argument a, FreeVariable x) 
	{
		return new Atomic(body.subst(a,x));
	}

	public Expression subst(Type T, FreeTypeVariable X) {
		return new Atomic(body.subst(T,X));
	}
	
	@Override
	public Set<Variable> vars() {
		return body.vars();
	}

	@Override
	public orc.ast.oil.expression.Expression convert(Env<Variable> vars, Env<TypeVariable> typevars) throws CompilationException {
		return new orc.ast.oil.expression.Atomic(body.convert(vars, typevars));
	}

	public String toString() {
		return "(atomic (" + body + "))";
	}
	
}
