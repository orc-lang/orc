package orc.ast.simple.expression;

import java.util.Set;
import java.util.HashSet;

import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.FreeVariable;
import orc.ast.simple.argument.Variable;
import orc.ast.simple.type.FreeTypeVariable;
import orc.ast.simple.type.Type;
import orc.ast.simple.type.TypeVariable;
import orc.env.Env;
import orc.error.compiletime.CompilationException;

public class Throw extends Expression {

	Expression exception;
	
	public Throw(Expression exception){
		this.exception = exception;
	}

	public Expression subst(Argument a, FreeVariable x) 
	{
		return new Throw(exception.subst(a,x));
	}
	
	/* (non-Javadoc)
	 * @see orc.ast.simple.expression.Expression#subst(orc.ast.simple.type.Type, orc.ast.simple.type.FreeTypeVariable)
	 */
	@Override
	public Expression subst(Type T, FreeTypeVariable X) {
		return new Throw(exception.subst(T,X));
	}
	
	public Set<Variable> vars(){
		Set s =  new HashSet<Variable>();
		s.add(exception.vars());
		return s;
	}
	
	public orc.ast.oil.expression.Expression convert(Env<Variable> vars, Env<TypeVariable> typevars) throws CompilationException {
		return new orc.ast.oil.expression.Throw(exception.convert(vars, typevars));
	}
}
