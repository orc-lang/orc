package orc.ast.simple.expression;

import java.util.HashSet;
import java.util.Set;

import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.NamedVariable;
import orc.ast.simple.argument.Variable;
import orc.env.Env;
import orc.runtime.nodes.Node;

public class Stop extends Expression {


	@Override
	public Expression subst(Argument a, NamedVariable x) {
		return this;
	}

	@Override
	public Set<Variable> vars() {
		return new HashSet<Variable>();
	}

	@Override
	public orc.ast.oil.expression.Expression convert(Env<Variable> vars, Env<String> typevars) {
		
		return new orc.ast.oil.expression.Stop();
	}
	
}
