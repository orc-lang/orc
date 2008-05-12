package orc.ast.extended;

/**
 * An expression executed at a remote server (given by the second argument).
 * @author quark
 */
public class Remote extends Expression {

	public Expression expression;
	public Expression server;
	
	public Remote(Expression server, Expression expression) {
		this.expression = expression;
		this.server = server;
	}

	@Override
	public orc.ast.simple.Expression simplify() {
		// This operator is a little weird. The server (right operand) must be
		// evaluated, but the expression (right operand) is not.
		Arg locationArg = server.argify();
		orc.ast.simple.Expression e = new orc.ast.simple.Remote(
				locationArg.asArg(), expression.simplify());
		return locationArg.bind(e);
	}

}