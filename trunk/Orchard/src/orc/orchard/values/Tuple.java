package orc.orchard.values;

import java.util.Arrays;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

public class Tuple extends Value {
	@XmlElement(name="element")
	public Object[] elements = new Object[]{};
	@XmlAttribute
	public int size;
	public Tuple() {}
	public Tuple(Object[] elements) {
		this.size = elements.length;
		this.elements = elements;
	}
	public String toString() {
		return super.toString() + "(" + Arrays.toString(elements) + ")";
	}
}
