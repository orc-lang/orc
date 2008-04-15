package orc.ast.simple;

import java.util.HashSet;
import java.util.Set;

import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;
import orc.runtime.nodes.Node;

public class Silent extends Expression {

	@Override
	public Node compile(Node output) {
		return new orc.runtime.nodes.Silent();
	}

	@Override
	public Expression subst(Argument a, NamedVar x) {
		return this;
	}

	@Override
	public Set<Var> vars() {
		return new HashSet<Var>();
	}
	
}
