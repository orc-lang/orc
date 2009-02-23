package orc.ast.oil.xml;

import java.net.URI;

import javax.xml.bind.annotation.XmlAttribute;

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
	public orc.ast.oil.arg.Arg unmarshal() {
		return new orc.ast.oil.arg.Site(orc.ast.sites.Site.build(protocol, location));
	}
}
