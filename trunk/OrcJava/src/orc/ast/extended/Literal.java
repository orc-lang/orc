package orc.ast.extended;

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

}
