package orc.ast.oil;

import java.util.Set;

import orc.ast.oil.xml.Expression;
import orc.env.Env;
import orc.error.Located;
import orc.error.SourceLocation;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.typing.TypeException;
import orc.runtime.nodes.Node;
import orc.type.Type;

/**
 * Annotate an expression with a source location.
 * @author quark
 */
public class WithLocation extends Expr implements Located {
	public final Expr body;
	public final SourceLocation location;
	public WithLocation(Expr expr, SourceLocation location) {
		this.body = expr;
		this.location = location;
	}
	public SourceLocation getSourceLocation() {
		return location;
	}

	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}

	@Override
	public Type typesynth(Env<Type> ctx, Env<Type> typectx) throws TypeException {
		try {
			return body.typesynth(ctx, typectx);
		}
		catch (TypeException e) {
			/* If this error has no location, give it this (least enclosing) location */
			if (e.getSourceLocation() == null || e.getSourceLocation().isUnknown()) {
				e.setSourceLocation(location);
			}
			throw e;
		}
	}
	
	@Override
	public void typecheck(Type T, Env<Type> ctx, Env<Type> typectx) throws TypeException {
		try {
			body.typecheck(T, ctx, typectx);
		}
		catch (TypeException e) {
			/* If this error has no location, give it this (least enclosing) location */
			if (e.getSourceLocation() == null || e.getSourceLocation().isUnknown()) {
				e.setSourceLocation(location);
			}
			throw e;
		}
	}
	
	public String toString() {
		return "{-" + location + "-}(" + body +")";
	}
	
	@Override
	public void addIndices(Set<Integer> indices, int depth) {
		body.addIndices(indices, depth);
	}
	
	@Override
	public Expression marshal() throws CompilationException {
		return new orc.ast.oil.xml.WithLocation(body.marshal(), location);
	}
}