package orc.ast.oil;

import java.util.Set;

import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;
import orc.env.Env;
import orc.error.compiletime.typing.TypeException;
import orc.runtime.nodes.Assign;
import orc.runtime.nodes.Node;
import orc.runtime.nodes.Unwind;
import orc.type.Type;

public class Push extends Expr {

	public Expr left;
	public Expr right;
	
	public Push(Expr left, Expr right)
	{
		this.left = left;
		this.right = right;
	}
	
	@Override
	public Node compile(Node output) {
		return left.compile(new Assign(right.compile(new Unwind(output))));
	}
	
	@Override
	public void addIndices(Set<Integer> indices, int depth) {
		left.addIndices(indices,depth); 
		right.addIndices(indices,depth+1); // Push binds a variable on the right
	}

	public String toString() {
		return "(" + left.toString() + " >> " + right.toString() + ")";
	}
	
	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
	
	@Override
	public Type typesynth(Env<Type> ctx) throws TypeException {
		Env<Type> rctx = ctx.clone();
		rctx.add(left.typesynth(ctx));
		return right.typesynth(rctx);
	}

	@Override
	public void typecheck(Type T, Env<Type> ctx) throws TypeException {
		Env<Type> rctx = ctx.clone();
		rctx.add(left.typesynth(ctx));
		right.typecheck(T, rctx);
	}
	
}
