package orc.ast.extended.expression;

import orc.ast.extended.Visitor;
import orc.ast.extended.type.AssertedType;
import orc.ast.extended.type.Type;
import orc.ast.simple.expression.WithLocation;
import orc.error.compiletime.CompilationException;

public class HasType extends Expression {

	public Expression body;
	public Type type;

	public HasType(Expression body, Type type)
	{
		this.body = body;
		this.type = type;
	}
	
	@Override
	public orc.ast.simple.expression.Expression simplify() throws CompilationException {
		
		boolean checkable = true;
		
		/* If this is an asserted type, it is not checkable */
		if (type instanceof AssertedType) {
			AssertedType atype = (AssertedType)type;
			type = atype.type;
			checkable = false;
		}
		
		return new WithLocation(
				new orc.ast.simple.expression.HasType(body.simplify(), type, checkable),
				getSourceLocation());
	}
	
	public String toString() {
		return "(" + body + " :: " + type + ")";
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
