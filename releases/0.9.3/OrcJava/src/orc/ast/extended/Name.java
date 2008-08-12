package orc.ast.extended;

public class Name extends Expression {

	public String name;
	
	public Name(String name)
	{
		this.name = name;
	}
	
	@Override
	public orc.ast.simple.Expression simplify() {
		orc.ast.simple.arg.Argument var = new orc.ast.simple.arg.NamedVar(name);
		var.setSourceLocation(getSourceLocation());
		return new orc.ast.simple.Let(var);
	}

	public Arg argify() {
		orc.ast.simple.arg.Argument var = new orc.ast.simple.arg.NamedVar(name);
		var.setSourceLocation(getSourceLocation());
		return new simpleArg(var);
	}
	
	public String toString() {
		return name;
	}
}
