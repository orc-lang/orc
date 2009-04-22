package orc.ast.oil.xml;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import orc.error.SourceLocation;

/**
 * FIXME: should include type info
 * @author quark
 */
public class Definition implements Serializable {
	@XmlAttribute(required=true)
	public int arity;
	@XmlElement(required=true)
	public Expression body;
	public SourceLocation location;
	@XmlAttribute(required=false)
	public String name;
	public Definition() {}
	public Definition(int arity, Expression body, SourceLocation location, String name) {
		this.arity = arity;
		this.body = body;
		this.location = location;
		this.name = name;
	}
	public String toString() {
		return super.toString() + "(" + arity + ", " + body + ")";
	}
	public orc.ast.oil.Def unmarshal() {
		return new orc.ast.oil.Def(arity, body.unmarshal(), 0, null, null, location, name);
	}
}
