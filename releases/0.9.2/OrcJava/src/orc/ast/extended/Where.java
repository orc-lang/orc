package orc.ast.extended;

import orc.ast.extended.pattern.Pattern;

public class Where extends Expression {

	public Expression left;
	public Expression right;
	public Pattern p;
	
	public Where(Expression left, Expression right, Pattern p)
	{
		this.left = left;
		this.right = right;
		this.p = p;
	}
	
	@Override
	public orc.ast.simple.Expression simplify() {
		
		orc.ast.simple.Expression source = right.simplify();
		orc.ast.simple.Expression target = left.simplify();
		orc.ast.simple.arg.Var t = new orc.ast.simple.arg.Var();
		
		source = Pattern.filter(p.match(source));
		target = p.bind(t, target);
		
		return new orc.ast.simple.Where(target, source,t);
	}

}
