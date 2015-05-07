package orc.ast.xml.expression.argument;

import java.net.URI;

import javax.xml.bind.annotation.XmlAttribute;

import orc.Config;
import orc.error.compiletime.CompilationException;

public class Site extends Argument {
	@XmlAttribute(required=true)
	public String protocol;
	@XmlAttribute(required=true)
	public URI location;
	public Site() {}
	public Site(String protocol, URI location) {
		this.protocol = protocol;
		this.location = location;
	}
	public String toString() {
		return super.toString() + "(" + protocol + ", " + location + ")";
	}
	@Override
	public orc.ast.oil.expression.argument.Argument unmarshal(Config config) throws CompilationException {
		return new orc.ast.oil.expression.argument.Site(orc.ast.sites.Site.build(protocol, location));
	}
}
