package orc.orchard.oil;

import orc.orchard.errors.InvalidOilException;

public class Pull extends Expression {
	public Expression left;
	public Expression right;
	public Pull() {}
	public Pull(Expression left, Expression right) {
		this.left = left;
		this.right = right;
	}
	public String toString() {
		return super.toString() + "(" + left + ", " + right + ")";
	}
	@Override
	public orc.ast.oil.Expr unmarshal() throws InvalidOilException {
		return new orc.ast.oil.Pull(left.unmarshal(), right.unmarshal());
	}
}
