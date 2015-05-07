package orc.ast.extended;

import xtc.util.Utilities;

public class Literal extends Expression {

	Object val;
	
	public Literal(Object val)
	{
		this.val = val;
	}
	
	@Override
	public orc.ast.simple.Expression simplify() {
		return new orc.ast.simple.Let(new orc.ast.simple.arg.Constant(val));
	}
	
	public Arg argify() {
		return new simpleArg(new orc.ast.simple.arg.Constant(val));
	}
	
	public String toString() {
		if (val == null) {
			return String.valueOf(val);
		} else if (val instanceof String) {
			return '"' + Utilities.escape((String)val, Utilities.JAVA_ESCAPES) + '"';
		} else return String.valueOf(val);
	}
}
