package orc.ast.oil.expression;

import java.util.Set;

import orc.ast.oil.ContextualVisitor;
import orc.ast.oil.Visitor;
import orc.error.Located;
import orc.error.SourceLocation;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.typing.TypeException;
import orc.runtime.nodes.Node;
import orc.type.Type;
import orc.type.TypingContext;

/**
 * Annotate an expression with a source location.
 * @author quark
 */
public class WithLocation extends Expression implements Located {
	public final Expression body;
	public final SourceLocation location;
	public WithLocation(Expression expr, SourceLocation location) {
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
	
	public <E,C> E accept(ContextualVisitor<E,C> cvisitor, C initialContext) {
		return cvisitor.visit(this, initialContext);
	}

	@Override
	public Type typesynth(TypingContext ctx) throws TypeException {
		try {
			return body.typesynth(ctx);
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
	public void typecheck(TypingContext ctx, Type T) throws TypeException {
		try {
			body.typecheck(ctx, T);
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
		return "{-" + location + "-}\n(" + body +")";
	}
	
	@Override
	public void addIndices(Set<Integer> indices, int depth) {
		body.addIndices(indices, depth);
	}
	
	@Override
	public orc.ast.xml.expression.Expression marshal() throws CompilationException {
		return new orc.ast.xml.expression.WithLocation(body.marshal(), location);
	}
}
