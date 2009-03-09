package orc.ast.oil;

import java.util.Set;

import orc.env.Env;
import orc.error.compiletime.typing.TypeException;
import orc.runtime.nodes.Fork;
import orc.runtime.nodes.Node;
import orc.runtime.nodes.Store;
import orc.type.Type;

public class Atomic extends Expr {

	public Expr body;
	
	public Atomic(Expr body)
	{
		this.body = body;
	}

	@Override
	public void addIndices(Set<Integer> indices, int depth) {
		body.addIndices(indices, depth);
	}
	
	public String toString() {
		return "(atomic (" + body + "))";
	}
	
	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}

	@Override
	public Type typesynth(Env<Type> ctx, Env<Type> typectx) throws TypeException {
		return body.typesynth(ctx, typectx);
	}

	@Override
	public void typecheck(Type T, Env<Type> ctx, Env<Type> typectx) throws TypeException {
		body.typecheck(T, ctx, typectx);
	}

}
