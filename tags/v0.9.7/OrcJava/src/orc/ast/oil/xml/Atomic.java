package orc.ast.oil.xml;

import javax.xml.bind.annotation.XmlElement;

public class Atomic extends Expression {
	@XmlElement(required=true)
	public Expression body;
	public Atomic() {}
	public Atomic(Expression body) {
		this.body = body;
	}
	public String toString() {
		return "(atomic " + body.toString() + ")";
	}
	@Override
	public orc.ast.oil.Expr unmarshal() {
		return new orc.ast.oil.Atomic(body.unmarshal());
	}
}
