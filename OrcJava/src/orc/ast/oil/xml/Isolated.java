package orc.ast.oil.xml;

import javax.xml.bind.annotation.XmlElement;

/**
 * The "isolated" keyword.
 * @see orc.runtime.nodes.Isolate
 * @author quark
 */
public class Isolated extends Expression {
	@XmlElement(required=true)
	public Expression body;
	public Isolated() {}
	public Isolated(Expression body) {
		this.body = body;
	}
	public String toString() {
		return "(isolated " + body.toString() + ")";
	}
	@Override
	public orc.ast.oil.Expr unmarshal() {
		return new orc.ast.oil.Isolated(body.unmarshal());
	}
}
