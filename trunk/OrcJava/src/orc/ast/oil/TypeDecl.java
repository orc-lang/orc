package orc.ast.oil;

import java.util.Set;

import orc.env.Env;
import orc.error.compiletime.typing.TypeException;
import orc.runtime.nodes.Node;
import orc.type.Type;

/**
 * 
 * Bind a type in the given scope.
 * 
 * @author dkitchin
 *
 */
public class TypeDecl extends Expr {

	public Type type;
	public Expr body;
	
	public TypeDecl(Type type, Expr body) {
		this.type = type;
		this.body = body;
	}

	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}

	@Override
	public void addIndices(Set<Integer> indices, int depth) {
		body.addIndices(indices, depth);
	}

	@Override
	public Node compile(Node output) {
		/* Discard the type information; it is not used at runtime. */
		return body.compile(output);
	}

	@Override
	public Type typesynth(Env<Type> ctx, Env<Type> typectx) throws TypeException {
		Type actualType = type.subst(typectx);
		return body.typesynth(ctx, typectx.extend(actualType));
	}

	public void typecheck(Type T, Env<Type> ctx, Env<Type> typectx) throws TypeException {
		Type actualType = type.subst(typectx);
		body.typecheck(T, ctx, typectx.extend(actualType));
	}
}
