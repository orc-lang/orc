package orc.ast.extended;

public class Semi extends Expression {

	public Expression left;
	public Expression right;

	public Semi(Expression left, Expression right)
	{
		this.left = left;
		this.right = right;
	}
	
	@Override
	public orc.ast.simple.Expression simplify() {
		return new orc.ast.simple.Semi(left.simplify(), right.simplify());
	}

}
