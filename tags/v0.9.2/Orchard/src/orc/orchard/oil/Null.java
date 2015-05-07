package orc.orchard.oil;

public class Null extends Expression {
	@Override
	public orc.ast.oil.Expr unmarshal() {
		return new orc.ast.oil.Null();
	}
}
