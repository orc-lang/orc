package orc.ast.oil;

import orc.env.Env;
import orc.error.Located;
import orc.error.SourceLocation;
import orc.error.compiletime.typing.TypeException;
import orc.runtime.nodes.Node;
import orc.type.Type;

/**
 * Annotate an expression with a source location.
 * @author quark
 */
public class WithLocation extends Expr implements Located {
	public final Expr expr;
	public final SourceLocation location;
	public WithLocation(Expr expr, SourceLocation location) {
		this.expr = expr;
		this.location = location;
	}
	public SourceLocation getSourceLocation() {
		return location;
	}
	@Override
	public Node compile(Node output) {
		return new orc.runtime.nodes.WithLocation(expr.compile(output), location);
	}

	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}

	@Override
	public Type typesynth(Env<Type> ctx) throws TypeException {
		return expr.typesynth(ctx);
	}
	
	public String toString() {
		return "{-" + location + "-}(" + expr +")";
	}
}
