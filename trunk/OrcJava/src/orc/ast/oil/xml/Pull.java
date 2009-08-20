package orc.ast.oil.xml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import orc.Config;
import orc.error.compiletime.CompilationException;

public class Pull extends Expression {
	@XmlElement(required=true)
	public Expression left;
	@XmlElement(required=true)
	public Expression right;
	@XmlAttribute(required=false)
	public String name;
	public Pull() {}
	public Pull(Expression left, Expression right, String name) {
		this.left = left;
		this.right = right;
		this.name = name;
	}
	public String toString() {
		return super.toString() + "(" + left + ", " + right + ")";
	}
	@Override
	public orc.ast.oil.expression.Expr unmarshal(Config config) throws CompilationException {
		return new orc.ast.oil.expression.Pruning(left.unmarshal(config), right.unmarshal(config), name);
	}
}
