package orc.ast.oil;

import java.util.Set;

import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;
import orc.env.Env;
import orc.error.compiletime.typing.TypeException;
import orc.runtime.nodes.Node;
import orc.runtime.nodes.Store;
import orc.runtime.nodes.Unwind;
import orc.type.Type;

public class Pull extends Expr {

	public Expr left;
	public Expr right;
	
	public Pull(Expr left, Expr right)
	{
		this.left = left;
		this.right = right;
	}
	
	
	@Override
	public Node compile(Node output) {	
		return new orc.runtime.nodes.Subgoal(left.compile(new Unwind(output)), right.compile(new Store()));
	}

	
	@Override
	public void addIndices(Set<Integer> indices, int depth) {
		left.addIndices(indices,depth+1); // Pull binds a variable on the left
		right.addIndices(indices,depth);
	}

	public String toString() {
		return "(" + left.toString() + " << " + right.toString() + ")";
	}
	
	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
	
	@Override
	public Type typesynth(Env<Type> ctx) throws TypeException {
		
		Type R = right.typesynth(ctx);
		Type L = left.typesynth(ctx.add(R));
		return L;
	}

	@Override
	public void typecheck(Type T, Env<Type> ctx) throws TypeException {
		
		Type R = right.typesynth(ctx);
		left.typecheck(T, ctx.add(R));
	}
}
