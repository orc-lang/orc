package orc.ast.extended;

import orc.ast.simple.WithLocation;
import orc.error.compiletime.CompilationException;

/**
 * @author quark
 */
public class Dereference extends Expression {

	public Expression target;
	
	public Dereference(Expression target) {
		this.target = target;
	}
	
	@Override
	public orc.ast.simple.Expression simplify() throws CompilationException {
		Call call1 = new Call(target, new Field("read"));
		call1.setSourceLocation(getSourceLocation());
		Call call2 = new Call(call1);
		call2.setSourceLocation(getSourceLocation());
		return call2.simplify();
	}

	public String toString() {
		return target + "?";
	}
}
