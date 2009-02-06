package orc.orchard.oil;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlAttribute;

import orc.error.SourceLocation;
import orc.orchard.errors.InvalidOilException;

/**
 * FIXME: should include type info
 * @author quark
 */
public class Definition implements Serializable {
	@XmlAttribute
	public int arity;
	public Expression body;
	public SourceLocation location;
	public Definition() {}
	public Definition(int arity, Expression body, SourceLocation location) {
		this.arity = arity;
		this.body = body;
		this.location = location;
	}
	public String toString() {
		return super.toString() + "(" + arity + ", " + body + ")";
	}
	public orc.ast.oil.Def unmarshal() throws InvalidOilException {
		return new orc.ast.oil.Def(arity, body.unmarshal(), 0, null, null, location);
	}
}
