package orc.ast.oil.expression;

import java.util.Set;

import orc.ast.oil.Visitor;
import orc.ast.oil.xml.Expression;
import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.NamedVar;
import orc.ast.simple.argument.Var;
import orc.env.Env;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.typing.TypeException;
import orc.runtime.nodes.Node;
import orc.runtime.nodes.Store;
import orc.runtime.nodes.Unwind;
import orc.type.Type;

public class Pruning extends Expr {

	public Expr left;
	public Expr right;
	
	/* An optional variable name, used for documentation purposes.
	 * It has no operational purpose, since the expression is already
	 * in deBruijn index form. 
	 */
	public String name;
	
	public Pruning(Expr left, Expr right, String name)
	{
		this.left = left;
		this.right = right;
		this.name = name;
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
	public Type typesynth(Env<Type> ctx, Env<Type> typectx) throws TypeException {
		
		Type rtype = right.typesynth(ctx, typectx);
		return left.typesynth(ctx.extend(rtype), typectx);
	}

	@Override
	public void typecheck(Type T, Env<Type> ctx, Env<Type> typectx) throws TypeException {
		Env<Type> lctx = ctx.clone();
		lctx.add(right.typesynth(ctx, typectx));
		left.typecheck(T, lctx, typectx);
	}

	@Override
	public Expression marshal() throws CompilationException {
		return new orc.ast.oil.xml.Pull(left.marshal(), right.marshal(), name);
	}
}
