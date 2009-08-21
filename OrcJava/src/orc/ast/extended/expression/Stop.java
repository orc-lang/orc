package orc.ast.extended.expression;

import orc.ast.extended.Visitor;
import orc.ast.simple.expression.WithLocation;

public class Stop extends Expression {

	@Override
	public orc.ast.simple.expression.Expression simplify() {
		return new WithLocation(
				new orc.ast.simple.expression.Stop(),
				getSourceLocation());
	}
	public String toString() {
		return "stop";
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
