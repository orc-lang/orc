package orc.ast.simple;

import java.util.HashSet;
import java.util.Set;

import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.FreeVar;
import orc.ast.simple.arg.Var;
import orc.runtime.nodes.Node;

public class Zero extends Expression {

	@Override
	public Node compile(Node output) {
		return new orc.runtime.nodes.Zero();
	}

	@Override
	public void subst(Argument a, FreeVar x) {
		// Do nothing.
	}

	@Override
	public Set<Var> vars() {
		return new HashSet<Var>();
	}
	
}
