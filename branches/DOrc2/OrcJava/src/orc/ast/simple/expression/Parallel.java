package orc.ast.simple.expression;

import java.util.Set;

import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.FreeVariable;
import orc.ast.simple.argument.Variable;
import orc.ast.simple.type.FreeTypeVariable;
import orc.ast.simple.type.Type;
import orc.ast.simple.type.TypeVariable;
import orc.env.Env;
import orc.error.compiletime.CompilationException;

public class Parallel extends Expression {

	Expression left;
	Expression right;
	
	public Parallel(Expression left, Expression right)
	{
		this.left = left;
		this.right = right;
	}
	
	@Override
	public Expression subst(Argument a, FreeVariable x) 
	{
		return new Parallel(left.subst(a,x),right.subst(a,x));
	}

	/* (non-Javadoc)
	 * @see orc.ast.simple.expression.Expression#subst(orc.ast.simple.type.Type, orc.ast.simple.type.FreeTypeVariable)
	 */
	@Override
	public Expression subst(Type T, FreeTypeVariable X) {
		return new Parallel(left.subst(T,X),right.subst(T,X));
	}
	
	@Override
	public Set<Variable> vars() {
		
		Set<Variable> s = left.vars();
		s.addAll(right.vars());
		return s;
	}

	@Override
	public orc.ast.oil.expression.Expression convert(Env<Variable> vars, Env<TypeVariable> typevars) throws CompilationException {
		return new orc.ast.oil.expression.Parallel(left.convert(vars, typevars), right.convert(vars, typevars));
	}
	
	public String toString() {
		return "(" + left + " | " + right + ")";
	}

}
