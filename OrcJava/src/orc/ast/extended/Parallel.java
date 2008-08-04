package orc.ast.extended;

import orc.error.compiletime.CompilationException;

public class Parallel extends Expression {

	public Expression left;
	public Expression right;

	public Parallel(Expression left, Expression right)
	{
		this.left = left;
		this.right = right;
	}
	
	@Override
	public orc.ast.simple.Expression simplify() throws CompilationException {
		return new orc.ast.simple.Parallel(left.simplify(), right.simplify());
	}
	
	public String toString() {
		return "(" + left + " | " + right + ")";
	}
}
