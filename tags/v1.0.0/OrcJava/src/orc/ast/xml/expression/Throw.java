package orc.ast.xml.expression;

import javax.xml.bind.annotation.XmlElement;

import orc.Config;
import orc.error.compiletime.CompilationException;

/**
 * The "isolated" keyword.
 * @see orc.runtime.nodes.Isolate
 * @author quark
 */
public class Throw extends Expression {
	@XmlElement(required=true)
	public Expression body;
	public Throw() {}
	public Throw(Expression body) {
		this.body = body;
	}
	public String toString() {
		return "(isolated " + body.toString() + ")";
	}
	@Override
	public orc.ast.oil.expression.Expression unmarshal(Config config) throws CompilationException {
		return new orc.ast.oil.expression.Throw(body.unmarshal(config));
	}
}
