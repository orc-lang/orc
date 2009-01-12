package orc.orchard.oil;

import orc.orchard.errors.InvalidOilException;

public class Atomic extends Expression {
	public Expression body;
	public Atomic() {}
	public Atomic(Expression body) {
		this.body = body;
	}
	public String toString() {
		return "(atomic " + body.toString() + ")";
	}
	@Override
	public orc.ast.oil.Expr unmarshal() throws InvalidOilException {
		return new orc.ast.oil.Atomic(body.unmarshal());
	}
}
