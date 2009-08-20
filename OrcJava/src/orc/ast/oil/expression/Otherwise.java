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
import orc.runtime.nodes.Leave;
import orc.runtime.nodes.Node;
import orc.type.Type;

public class Otherwise extends Expr {

	public Expr left;
	public Expr right;
	
	public Otherwise(Expr left, Expr right)
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
		return "(" + left.toString() + " ; " + right.toString() + ")";
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
		return new orc.ast.oil.xml.Semicolon(left.marshal(), right.marshal());
	}
}
