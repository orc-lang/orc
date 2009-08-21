package orc.ast.simple.expression;

import java.util.Set;

import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.NamedVariable;
import orc.ast.simple.argument.Variable;
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
	public Expression subst(Argument a, NamedVariable x)  {
		return new Isolated(body.subst(a,x));
	}

	@Override
	public Set<Variable> vars() {
		return body.vars();
	}

	@Override
	public orc.ast.oil.expression.Expression convert(Env<Variable> vars, Env<String> typevars) throws CompilationException {
		return new orc.ast.oil.expression.Isolated(body.convert(vars, typevars));
	}

}
