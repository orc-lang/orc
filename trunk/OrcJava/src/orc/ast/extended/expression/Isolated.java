package orc.ast.extended.expression;

import orc.ast.extended.Visitor;
import orc.ast.simple.expression.WithLocation;
import orc.error.compiletime.CompilationException;

/**
 * The "isolated" keyword.
 * @see orc.ast.simple.expression.Isolated
 * @author quark
 */
public class Isolated extends Expression {

	public Expression body;

	public Isolated(Expression body) {
		this.body = body;
	}
	
	@Override
	public orc.ast.simple.expression.Expression simplify() throws CompilationException {
		return new WithLocation(
				new orc.ast.simple.expression.Isolated(body.simplify()),
				getSourceLocation());
	}
	
	public String toString() {
		return "(isolated (" + body + "))";
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
