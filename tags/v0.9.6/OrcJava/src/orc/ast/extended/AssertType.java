package orc.ast.extended;

import orc.ast.simple.WithLocation;
import orc.ast.simple.type.Type;
import orc.error.compiletime.CompilationException;

public class AssertType extends Expression {

	public Expression body;
	public Type type;

	public AssertType(Expression body, Type type)
	{
		this.body = body;
		this.type = type;
	}
	
	@Override
	public orc.ast.simple.Expression simplify() throws CompilationException {
		return new WithLocation(
				new orc.ast.simple.HasType(body.simplify(), type, false),
				getSourceLocation());
	}
	
	public String toString() {
		return "(" + body + " :!: " + type + ")";
	}
}
