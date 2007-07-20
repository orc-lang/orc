package orc.ast.extended;

public class Name extends Expression {

	public String name;
	
	public Name(String name)
	{
		this.name = name;
	}
	
	@Override
	public orc.ast.simple.Expression simplify() {
		return new orc.ast.simple.Call(new orc.ast.simple.arg.FreeVar(name));
	}

	public Arg argify() {
		return new simpleArg(new orc.ast.simple.arg.FreeVar(name));
	}
	
}
