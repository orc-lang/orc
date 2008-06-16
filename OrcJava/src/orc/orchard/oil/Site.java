package orc.orchard.oil;

import java.net.URI;

import javax.xml.bind.annotation.XmlAttribute;

import orc.orchard.InvalidOilException;

public class Site extends Argument {
	@XmlAttribute
	public String protocol;
	@XmlAttribute
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
	public orc.ast.oil.arg.Arg unmarshal() throws InvalidOilException {
		return new orc.ast.oil.arg.Site(orc.ast.sites.Site.build(protocol, location));
	}
}
