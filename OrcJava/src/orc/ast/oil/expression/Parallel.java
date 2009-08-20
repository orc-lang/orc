package orc.ast.oil.expression;

import java.util.Set;

import orc.ast.oil.Visitor;
import orc.ast.oil.xml.Expression;
import orc.env.Env;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.typing.TypeException;
import orc.runtime.nodes.Fork;
import orc.runtime.nodes.Node;
import orc.type.Type;

public class Parallel extends Expr {

	public Expr left;
	public Expr right;
	
	public Parallel(Expr left, Expr right)
	{
		this.left = left;
		this.right = right;
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

	@Override
	public Expression marshal() throws CompilationException {
		return new orc.ast.oil.xml.Bar(left.marshal(), right.marshal());
	}
}
