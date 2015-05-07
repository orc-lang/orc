package orc.ast.extended.expression;

import orc.ast.extended.Visitor;

public class Field extends Expression {

	public String field;
	
	public Field(String field)
	{
		this.field = field;
	}
	
	@Override
	public orc.ast.simple.expression.Expression simplify() {
		throw new Error("Field accesses can only occur in argument position");
	}
	
	public Arg argify() {
		return new simpleArg(new orc.ast.simple.argument.Field(field));
	}

	public String toString() {
		return field;
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
