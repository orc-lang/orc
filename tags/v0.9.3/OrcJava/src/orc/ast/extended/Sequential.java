package orc.ast.extended;

import orc.ast.extended.pattern.Pattern;
import orc.ast.extended.pattern.PatternVisitor;
import orc.ast.extended.pattern.WildcardPattern;
import orc.ast.simple.arg.Var;
import orc.error.compiletime.CompilationException;

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
	
	public Sequential(Expression left, Expression right)
	{
		this(left, right, new WildcardPattern());
	}
	
	@Override
	public orc.ast.simple.Expression simplify() throws CompilationException {
		
		orc.ast.simple.Expression source = left.simplify();
		orc.ast.simple.Expression target = right.simplify();
		
		Var s = new Var();
		Var t = new Var();
		
		PatternVisitor pv = p.process(s);
		
		source = new orc.ast.simple.Sequential(source, pv.filter(), s);
		target = pv.target(t, target);
		
		return new orc.ast.simple.Sequential(source, target, t);
	}
	
	public String toString() {
		return "(" + left + " >"+p+"> " + right + ")";
	}
}
