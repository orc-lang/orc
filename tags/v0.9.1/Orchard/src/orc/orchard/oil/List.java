package orc.orchard.oil;

import java.util.Arrays;

import javax.xml.bind.annotation.XmlElement;

import orc.ast.oil.arg.Arg;
import orc.orchard.errors.InvalidOilException;

public class List extends Value {
	@XmlElement(name="element")
	public Value[] elements = new Value[]{};
	public List() {}
	public List(Value[] elements) {
		this.elements = elements;
	}
	public String toString() {
		return super.toString() + "(" + Arrays.toString(elements) + ")";
	}
	@Override
	public Arg unmarshal() throws InvalidOilException {
		throw new InvalidOilException("List " + this.toString() + " cannot be unmarshaled.");
	}
}
