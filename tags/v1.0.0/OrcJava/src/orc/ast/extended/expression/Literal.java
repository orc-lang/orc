package orc.ast.extended.expression;

import orc.ast.extended.Visitor;
import xtc.util.Utilities;

public class Literal extends Expression {

	Object val;
	
	public Literal(Object val)
	{
		this.val = val;
	}
	
	@Override
	public orc.ast.simple.expression.Expression simplify() {
		return new orc.ast.simple.expression.Let(new orc.ast.simple.argument.Constant(val));
	}
	
	public Arg argify() {
		return new simpleArg(new orc.ast.simple.argument.Constant(val));
	}
	
	public String toString() {
		if (val == null) {
			return String.valueOf(val);
		} else if (val instanceof String) {
			return '"' + Utilities.escape((String)val, Utilities.JAVA_ESCAPES) + '"';
		} else return String.valueOf(val);
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
