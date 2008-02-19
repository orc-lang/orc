package orc.ast.extended;

import orc.ast.extended.pattern.Pattern;

public class Sequential extends Expression {

	public Expression left;
	public Expression right;
	public Pattern p;
	
	public Sequential(Expression left, Expression right, Pattern p)
	{
		this.left = left;
		this.right = right;
		this.p = p;
	}
	
	@Override
	public orc.ast.simple.Expression simplify() {
		
		orc.ast.simple.Expression source = left.simplify();
		orc.ast.simple.Expression target = right.simplify();
		orc.ast.simple.arg.Var t = new orc.ast.simple.arg.Var();
		
		source = p.match(source);
		target = p.bind(target, t);
		
		return new orc.ast.simple.Sequential(source, target, t);
	}

}
