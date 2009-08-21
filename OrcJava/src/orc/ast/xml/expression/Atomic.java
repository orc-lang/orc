package orc.ast.xml.expression;

import javax.xml.bind.annotation.XmlElement;

import orc.Config;
import orc.error.compiletime.CompilationException;

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
	public orc.ast.oil.expression.Expression unmarshal(Config config) throws CompilationException {
		return new orc.ast.oil.expression.Atomic(body.unmarshal(config));
	}
}
