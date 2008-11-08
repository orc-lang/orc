package orc.ast.oil;

import java.util.HashSet;
import java.util.Set;

import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;
import orc.env.Env;
import orc.runtime.nodes.Node;
import orc.type.Type;

public class Silent extends Expr {

	@Override
	public Node compile(Node output) {
		return new orc.runtime.nodes.Silent();
	}
	
	public String toString() {
		return "stop";
	}
	
	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}

	@Override
	public Type typesynth(Env<Type> ctx) {
		return Type.BOT;
	}
	
	@Override
	public void typecheck(Type t, Env<Type> ctx) {
		// Do nothing. Silent checks against all types.
	}

	@Override
	public void addIndices(Set<Integer> indices, int depth) {
		return;
	}
}
