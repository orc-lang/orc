package orc.ast.xml.expression;

import javax.xml.bind.annotation.XmlElement;

import orc.Config;
import orc.error.compiletime.CompilationException;

public class Parallel extends Expression {
	@XmlElement(required=true)
	public Expression left;
	@XmlElement(required=true)
	public Expression right;
	public Parallel() {}
	public Parallel(Expression left, Expression right) {
		this.left = left;
		this.right = right;
	}
	public String toString() {
		return super.toString() + "(" + left + ", " + right + ")";
	}
	@Override
	public orc.ast.oil.expression.Expression unmarshal(Config config) throws CompilationException {
		return new orc.ast.oil.expression.Parallel(left.unmarshal(config), right.unmarshal(config));
	}
}
