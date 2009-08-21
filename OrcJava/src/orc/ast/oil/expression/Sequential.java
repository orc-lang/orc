package orc.ast.oil.expression;

import java.util.Set;

import orc.ast.oil.Visitor;
import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.NamedVariable;
import orc.ast.simple.argument.Variable;
import orc.env.Env;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.typing.TypeException;
import orc.runtime.nodes.Assign;
import orc.runtime.nodes.Node;
import orc.runtime.nodes.Unwind;
import orc.type.Type;

public class Sequential extends Expression {

	public Expression left;
	public Expression right;
	
	/* An optional variable name, used for documentation purposes.
	 * It has no operational purpose, since the expression is already
	 * in deBruijn index form. 
	 */
	public String name;
	
	public Sequential(Expression left, Expression right, String name)
	{
		this.left = left;
		this.right = right;
		this.name = name;
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
	public Type typesynth(Env<Type> ctx, Env<Type> typectx) throws TypeException {
		Env<Type> rctx = ctx.clone();
		rctx.add(left.typesynth(ctx, typectx));
		return right.typesynth(rctx, typectx);
	}

	@Override
	public void typecheck(Type T, Env<Type> ctx, Env<Type> typectx) throws TypeException {
		Env<Type> rctx = ctx.clone();
		rctx.add(left.typesynth(ctx, typectx));
		right.typecheck(T, rctx, typectx);
	}

	@Override
	public orc.ast.xml.expression.Expression marshal() throws CompilationException {
		return new orc.ast.xml.expression.Sequential(left.marshal(), right.marshal(), name);
	}
}
