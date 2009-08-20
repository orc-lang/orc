package orc.ast.simple.expression;

import java.util.HashSet;
import java.util.Set;

import orc.ast.oil.expression.Expr;
import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.NamedVar;
import orc.ast.simple.argument.Var;
import orc.env.Env;
import orc.runtime.nodes.Node;

public class Stop extends Expression {


	@Override
	public Expression subst(Argument a, NamedVar x) {
		return this;
	}

	@Override
	public Set<Var> vars() {
		return new HashSet<Var>();
	}

	@Override
	public Expr convert(Env<Var> vars, Env<String> typevars) {
		
		return new orc.ast.oil.expression.Stop();
	}
	
}
