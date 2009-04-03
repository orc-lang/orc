package orc.ast.extended;

import orc.ast.simple.WithLocation;
import orc.error.compiletime.CompilationException;

public class Atomic extends Expression {

	public Expression body;

	public Atomic(Expression body)
	{
		this.body = body;
	}
	
	@Override
	public orc.ast.simple.Expression simplify() throws CompilationException {
		return new WithLocation(
				new orc.ast.simple.Atomic(body.simplify()),
				getSourceLocation());
	}
	
	public String toString() {
		return "(atomic (" + body + "))";
	}
}
