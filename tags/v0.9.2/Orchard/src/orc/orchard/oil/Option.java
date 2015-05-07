package orc.orchard.oil;

import javax.xml.bind.annotation.XmlElement;

import orc.ast.oil.arg.Arg;
import orc.orchard.errors.InvalidOilException;

/**
 * Rather than using two different classes for NoneValue and SomeValue, we
 * assume use null to represent NoneValue, and Constant(null) to represent
 * a null SomeValue.
 * 
 * @author quark
 * 
 */
public class Option extends Value {
	@XmlElement(required=false)
	private Value value;
	public Option() {}
	public Option(Value value) {
		this.value = value;
	}
	public String toString() {
		return super.toString() + "(" + (value == null ? "null" : value) + ")";
	}
	@Override
	public Arg unmarshal() throws InvalidOilException {
		throw new InvalidOilException("Option " + this.toString() + " cannot be unmarshaled.");
	}
}
