package orc.ast.extended;

import orc.ast.extended.pattern.Pattern;
import orc.ast.simple.arg.Var;

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
		Var t = new Var();
		
		source = Pattern.filter(p.match(source));
		target = p.bind(t, target);
		
		return new orc.ast.simple.Sequential(source, target, t);
	}

}
