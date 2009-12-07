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

/**
 * The "isolated" keyword.
 * @see orc.ast.oil.expression.Isolated
 * @author quark
 */
public class Isolated extends Expression {

	Expression body;
	
	public Isolated(Expression body) {
		this.body = body;
	}
	
	@Override
	public Expression subst(Argument a, FreeVariable x)  {
		return new Isolated(body.subst(a,x));
	}
	
	/* (non-Javadoc)
	 * @see orc.ast.simple.expression.Expression#subst(orc.ast.simple.type.Type, orc.ast.simple.type.FreeTypeVariable)
	 */
	@Override
	public Expression subst(Type T, FreeTypeVariable X) {
		return new Isolated(body.subst(T, X));
	}

	@Override
	public Set<Variable> vars() {
		return body.vars();
	}

	@Override
	public orc.ast.oil.expression.Expression convert(Env<Variable> vars, Env<TypeVariable> typevars) throws CompilationException {
		return new orc.ast.oil.expression.Isolated(body.convert(vars, typevars));
	}

}
