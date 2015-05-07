package orc.ast.extended;

import orc.ast.simple.WithLocation;
import orc.error.compiletime.CompilationException;

/**
 * The "isolated" keyword.
 * @see orc.ast.simple.Isolated
 * @author quark
 */
public class Isolated extends Expression {

	public Expression body;

	public Isolated(Expression body) {
		this.body = body;
	}
	
	@Override
	public orc.ast.simple.Expression simplify() throws CompilationException {
		return new WithLocation(
				new orc.ast.simple.Isolated(body.simplify()),
				getSourceLocation());
	}
	
	public String toString() {
		return "(isolated (" + body + "))";
	}
}
