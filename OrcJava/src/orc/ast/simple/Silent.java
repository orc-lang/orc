package orc.ast.simple;

import java.util.HashSet;
import java.util.Set;

import orc.ast.oil.Expr;
import orc.ast.oil.Null;
import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;
import orc.env.Env;
import orc.runtime.nodes.Node;

public class Silent extends Expression {


	@Override
	public Expression subst(Argument a, NamedVar x) {
		return this;
	}

	@Override
	public Set<Var> vars() {
		return new HashSet<Var>();
	}

	@Override
	public Expr convert(Env<Var> vars) {
		
		return new Null();
	}
	
}
