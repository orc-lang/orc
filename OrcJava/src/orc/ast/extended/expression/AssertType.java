package orc.ast.extended.expression;

import orc.ast.extended.Visitor;
import orc.ast.extended.type.Type;
import orc.ast.simple.expression.WithLocation;
import orc.error.compiletime.CompilationException;

public class AssertType extends Expression {

	public Expression body;
	public Type type;

	public AssertType(Expression body, Type type)
	{
		this.body = body;
		this.type = type;
	}
	
	public String toString() {
		return "(" + body + " :!: " + type + ")";
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.expression.Expression#simplify()
	 */
	@Override
	public orc.ast.simple.expression.Expression simplify()
			throws CompilationException {
		return new orc.ast.simple.expression.WithLocation(
				new orc.ast.simple.expression.HasType(body.simplify(), type.simplify(), false),
				getSourceLocation());
	}
}
