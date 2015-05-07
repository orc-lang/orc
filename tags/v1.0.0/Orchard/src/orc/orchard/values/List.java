package orc.orchard.values;

import java.util.Arrays;

import javax.xml.bind.annotation.XmlElement;

public class List extends Value {
	@XmlElement(name="element")
	public Object[] elements = new Object[]{};
	public List() {}
	public List(Object[] elements) {
		this.elements = elements;
	}
	public String toString() {
		return super.toString() + "(" + Arrays.toString(elements) + ")";
	}
}
