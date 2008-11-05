package orc.ast.extended;

import orc.ast.simple.WithLocation;
import orc.error.compiletime.CompilationException;

/**
 * @author quark
 */
public class Assignment extends Expression {

	public Expression target;
	public Expression value;
	
	public Assignment(Expression target, Expression value)
	{
		this.target = target;
		this.value = value;
	}
	
	@Override
	public orc.ast.simple.Expression simplify() throws CompilationException {
		Call call1 = new Call(target, new Field("write"));
		call1.setSourceLocation(getSourceLocation());
		Call call2 = new Call(call1, value);
		call2.setSourceLocation(getSourceLocation());
		return call2.simplify();
	}

	public String toString() {
		return target + ":=" + value;
	}
}
