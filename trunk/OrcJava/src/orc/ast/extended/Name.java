package orc.ast.extended;

import orc.ast.simple.WithLocation;

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
		return new WithLocation(new orc.ast.simple.Let(var),
				getSourceLocation());
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
