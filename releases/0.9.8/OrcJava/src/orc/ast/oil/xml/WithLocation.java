package orc.ast.oil.xml;

import javax.xml.bind.annotation.XmlElement;

import orc.error.SourceLocation;

public class WithLocation extends Expression {
	@XmlElement(required=true)
	public Expression expr;
	@XmlElement(required=true)
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
	public orc.ast.oil.Expr unmarshal() {
		return new orc.ast.oil.WithLocation(expr.unmarshal(), location);
	}
}
