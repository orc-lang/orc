package orc.ast.extended.expression;

import orc.ast.extended.Visitor;
import orc.ast.extended.pattern.Pattern;
import orc.ast.extended.pattern.PatternSimplifier;
import orc.ast.simple.argument.Variable;
import orc.ast.simple.expression.WithLocation;
import orc.error.compiletime.CompilationException;
import orc.ast.extended.pattern.WildcardPattern;

public class Pruning extends Expression {

	public Expression left;
	public Expression right;
	public Pattern p;
	
	public Pruning(Expression left, Expression right, Pattern p)
	{
		this.left = left;
		this.right = right;
		this.p = p;
	}
	
	public Pruning(Expression left, Expression right)
	{
		this(left, right, new WildcardPattern());
	}
	
	@Override
	public orc.ast.simple.expression.Expression simplify() throws CompilationException {
		
		orc.ast.simple.expression.Expression source = right.simplify();
		orc.ast.simple.expression.Expression target = left.simplify();
		
		Variable s = new Variable();
		Variable t = new Variable();
		
		PatternSimplifier pv = p.process(s);
		
		source = new orc.ast.simple.expression.Sequential(source, pv.filter(), s);
		target = pv.target(t, target);
		
		return new WithLocation(
				new orc.ast.simple.expression.Pruning(target, source, t),
				getSourceLocation());
	}
	
	public String toString() {
		return "(" + left + " <"+p+"< " + right + ")";
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
