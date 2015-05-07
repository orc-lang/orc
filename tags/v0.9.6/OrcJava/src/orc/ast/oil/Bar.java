package orc.ast.oil;

import java.util.Set;

import orc.env.Env;
import orc.error.compiletime.typing.TypeException;
import orc.runtime.nodes.Fork;
import orc.runtime.nodes.Node;
import orc.type.Type;

public class Bar extends Expr {

	public Expr left;
	public Expr right;
	
	public Bar(Expr left, Expr right)
	{
		this.left = left;
		this.right = right;
	}
	
	@Override
	public Node compile(Node output) {
		return new Fork(left.compile(output), right.compile(output));
	}

	@Override
	public void addIndices(Set<Integer> indices, int depth) {
		left.addIndices(indices, depth);
		right.addIndices(indices, depth);
	}
	
	public String toString() {
		return "(" + left.toString() + " | " + right.toString() + ")";
	}
	
	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}

	@Override
	public Type typesynth(Env<Type> ctx, Env<Type> typectx) throws TypeException {
		
		Type L = left.typesynth(ctx, typectx);
		Type R = right.typesynth(ctx, typectx);
		return L.join(R);
	}

	@Override
	public void typecheck(Type T, Env<Type> ctx, Env<Type> typectx) throws TypeException {
		
		left.typecheck(T, ctx, typectx);
		right.typecheck(T, ctx, typectx);
	}

}
