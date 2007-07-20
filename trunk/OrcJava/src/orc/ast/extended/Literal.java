package orc.ast.extended;

public class Literal extends Expression {

	public Object val;
	
	public Literal(Object val)
	{
		this.val = val;
	}
	
	@Override
	public orc.ast.simple.Expression simplify() {
		
		if (val instanceof Integer && (Integer)val == 0)
		{ 
			return new orc.ast.simple.Zero();
		}
		else
		{
			throw new Error("Literal " + val.toString() + " cannot appear in call position.");
		}
	}
	
	public Arg argify() {
		return new simpleArg(new orc.ast.simple.arg.Constant(val));
	}

}
