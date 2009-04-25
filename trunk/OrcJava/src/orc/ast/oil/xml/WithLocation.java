package orc.ast.oil.xml;

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
	public orc.ast.oil.Expr unmarshal(Config config) throws CompilationException {
		return new orc.ast.oil.WithLocation(body.unmarshal(config), location);
	}
}
