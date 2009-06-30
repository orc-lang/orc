package orc.ast.simple;

import java.util.Set;

import orc.ast.oil.Bar;
import orc.ast.oil.Expr;
import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;
import orc.env.Env;
import orc.error.compiletime.CompilationException;

/**
 * The "isolated" keyword.
 * @see orc.ast.oil.Isolated
 * @author quark
 */
public class Isolated extends Expression {

	Expression body;
	
	public Isolated(Expression body) {
		this.body = body;
	}
	
	@Override
	public Expression subst(Argument a, NamedVar x)  {
		return new Isolated(body.subst(a,x));
	}

	@Override
	public Set<Var> vars() {
		return body.vars();
	}

	@Override
	public Expr convert(Env<Var> vars, Env<String> typevars) throws CompilationException {
		return new orc.ast.oil.Isolated(body.convert(vars, typevars));
	}

}
