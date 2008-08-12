package orc.ast.extended;

import orc.ast.extended.pattern.Pattern;
import orc.ast.extended.pattern.PatternVisitor;
import orc.ast.simple.arg.Var;
import orc.error.compiletime.CompilationException;
import orc.ast.extended.pattern.WildcardPattern;

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
	
	public Where(Expression left, Expression right)
	{
		this(left, right, new WildcardPattern());
	}
	
	@Override
	public orc.ast.simple.Expression simplify() throws CompilationException {
		
		orc.ast.simple.Expression source = right.simplify();
		orc.ast.simple.Expression target = left.simplify();
		
		Var s = new Var();
		Var t = new Var();
		
		PatternVisitor pv = p.process(s);
		
		source = new orc.ast.simple.Sequential(source, pv.filter(), s);
		target = pv.target(t, target);
		
		return new orc.ast.simple.Where(target, source, t);
	}
	
	public String toString() {
		return "(" + left + " <"+p+"< " + right + ")";
	}
}
