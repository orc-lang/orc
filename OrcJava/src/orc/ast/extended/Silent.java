package orc.ast.extended;

import orc.ast.simple.WithLocation;

public class Silent extends Expression {

	@Override
	public orc.ast.simple.Expression simplify() {
		return new WithLocation(
				new orc.ast.simple.Silent(),
				getSourceLocation());
	}
	public String toString() {
		return ".";
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
