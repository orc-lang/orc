package orc.ast.extended;

public class Literal extends Expression {

	public Object val;
	
	public Literal(Object val)
	{
		this.val = val;
	}
	
	@Override
	public orc.ast.simple.Expression simplify() {
		
		if (val instanceof Integer)
		{ 
			int i = ((Integer)val).intValue();
			
			if (i == 0) { return new orc.ast.simple.Zero(); } // "0" is silent
			if (i == 1) { return new orc.ast.simple.Let(); }  // "1" is signal
		}
		
		throw new Error("Literal " + val.toString() + " cannot appear in call position.");
	}
	
	public Arg argify() {
		return new simpleArg(new orc.ast.simple.arg.Constant(val));
	}

}
