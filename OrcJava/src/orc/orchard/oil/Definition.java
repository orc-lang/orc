package orc.orchard.oil;

import java.util.LinkedList;

import javax.xml.bind.annotation.XmlAttribute;

import orc.orchard.InvalidOilException;

public class Definition {
	@XmlAttribute
	public int arity;
	public Expression body;
	public Definition() {}
	public Definition(int arity, Expression body) {
		this.arity = arity;
		this.body = body;
	}
	public String toString() {
		return super.toString() + "(" + arity + ", " + body + ")";
	}
	public orc.ast.oil.Def unmarshal() throws InvalidOilException {
		return new orc.ast.oil.Def(arity, body.unmarshal());
	}
}
