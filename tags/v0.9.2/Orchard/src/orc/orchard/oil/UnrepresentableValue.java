package orc.orchard.oil;

import javax.xml.bind.annotation.XmlAttribute;

import orc.ast.oil.arg.Arg;
import orc.orchard.errors.InvalidOilException;

/**
 * FIXME: this is a hack so I can get everything to compile
 * without worrying about representations for weird values like
 * closures and sites. Long-term, this class should be removed.
 * 
 * @author quark
 */
public class UnrepresentableValue extends Value {
	@XmlAttribute
	private String description;
	public UnrepresentableValue() {}
	public UnrepresentableValue(String description) {
		this.description = description;
	}
	public String toString() {
		return super.toString() + "(" + description + ")";
	}
	@Override
	public Arg unmarshal() throws InvalidOilException {
		throw new InvalidOilException("Unvalue " + this.toString() + " cannot be unmarshaled.");
	}
}
