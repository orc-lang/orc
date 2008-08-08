package orc.orchard.oil;

public class Silent extends Expression {
	@Override
	public orc.ast.oil.Expr unmarshal() {
		return new orc.ast.oil.Silent();
	}
}
