package orc.ast.extended;

public class Field extends Expression {

	public String field;
	
	public Field(String field)
	{
		this.field = field;
	}
	
	@Override
	public orc.ast.simple.Expression simplify() {
		throw new Error("Field accesses can only occur in argument position");
	}
	
	public Arg argify() {
		return new simpleArg(new orc.ast.simple.arg.Field(field));
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
