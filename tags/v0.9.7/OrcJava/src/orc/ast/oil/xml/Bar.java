package orc.ast.oil.xml;

import javax.xml.bind.annotation.XmlElement;

public class Bar extends Expression {
	@XmlElement(required=true)
	public Expression left;
	@XmlElement(required=true)
	public Expression right;
	public Bar() {}
	public Bar(Expression left, Expression right) {
		this.left = left;
		this.right = right;
	}
	public String toString() {
		return super.toString() + "(" + left + ", " + right + ")";
	}
	@Override
	public orc.ast.oil.Expr unmarshal() {
		return new orc.ast.oil.Bar(left.unmarshal(), right.unmarshal());
	}
}
