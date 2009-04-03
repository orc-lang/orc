package orc.ast.oil.xml;

import javax.xml.bind.annotation.XmlElement;

public class Push extends Expression {
	@XmlElement(required=true)
	public Expression left;
	@XmlElement(required=true)
	public Expression right;
	public Push() {}
	public Push(Expression left, Expression right) {
		this.left = left;
		this.right = right;
	}
	public String toString() {
		return super.toString() + "(" + left + ", " + right + ")";
	}
	@Override
	public orc.ast.oil.Expr unmarshal() {
		return new orc.ast.oil.Push(left.unmarshal(), right.unmarshal(), null);
	}
}
