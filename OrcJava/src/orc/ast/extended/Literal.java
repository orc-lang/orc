package orc.ast.extended;

import orc.ast.val.Val;

public class Literal extends Expression {

	Val val;
	
	public Literal(Val val)
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

}
