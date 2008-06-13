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
		orc.runtime.sites.Site site;
		if (protocol.equals("orc")) {
			Class klass;
			try {
				klass = Class.forName(location.toString());
				site = (orc.runtime.sites.Site)klass.newInstance();
			} catch (ClassNotFoundException e) {
				throw new InvalidOilException(e);
			} catch (InstantiationException e) {
				throw new InvalidOilException(e);
			} catch (IllegalAccessException e) {
				throw new InvalidOilException(e);
			}
		} else {
			throw new InvalidOilException("Unrecognized protocol " + protocol);
		}
		return new orc.ast.oil.arg.Site(site);
	}
}
