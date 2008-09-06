package orc.orchard.oil;

import orc.error.SourceLocation;
import orc.orchard.errors.InvalidOilException;

public class WithLocation extends Expression {
	public Expression expr;
	public SourceLocation location;
	public WithLocation() {}
	public WithLocation(Expression expr, SourceLocation location) {
		this.expr = expr;
		this.location = location;
	}
	public String toString() {
		return expr.toString();
	}
	@Override
	public orc.ast.oil.Expr unmarshal() throws InvalidOilException {
		return new orc.ast.oil.WithLocation(expr.unmarshal(), location);
	}
}
