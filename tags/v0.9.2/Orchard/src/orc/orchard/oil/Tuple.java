package orc.orchard.oil;

import java.util.Arrays;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import orc.ast.oil.arg.Arg;
import orc.orchard.errors.InvalidOilException;

public class Tuple extends Value {
	@XmlElement(name="element")
	public Value[] elements = new Value[]{};
	@XmlAttribute
	public int size;
	public Tuple() {}
	public Tuple(Value[] elements) {
		this.size = elements.length;
		this.elements = elements;
	}
	public String toString() {
		return super.toString() + "(" + Arrays.toString(elements) + ")";
	}
	@Override
	public Arg unmarshal() throws InvalidOilException {
		throw new InvalidOilException("Tuple " + this.toString() + " cannot be unmarshaled.");
	}
}
