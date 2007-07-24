package orc.ast.extended;

public class Parallel extends Expression {

	public Expression left;
	public Expression right;

	public Parallel(Expression left, Expression right)
	{
		this.left = left;
		this.right = right;
	}
	
	@Override
	public orc.ast.simple.Expression simplify() {
		return new orc.ast.simple.Parallel(left.simplify(), right.simplify());
	}

}
