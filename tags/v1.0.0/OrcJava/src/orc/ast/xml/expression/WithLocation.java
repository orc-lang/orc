package orc.ast.xml.expression;

import javax.xml.bind.annotation.XmlElement;

import orc.Config;
import orc.error.SourceLocation;
import orc.error.compiletime.CompilationException;

public class WithLocation extends Expression {
	@XmlElement(required=true)
	public Expression body;
	@XmlElement(required=true)
	public SourceLocation location;
	public WithLocation() {}
	public WithLocation(Expression body, SourceLocation location) {
		this.body = body;
		this.location = location;
	}
	public String toString() {
		return body.toString();
	}
	@Override
	public orc.ast.oil.expression.Expression unmarshal(Config config) throws CompilationException {
		return new orc.ast.oil.expression.WithLocation(body.unmarshal(config), location);
	}
}
