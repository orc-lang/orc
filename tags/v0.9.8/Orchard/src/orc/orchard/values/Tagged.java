package orc.orchard.values;

import java.util.Arrays;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

public class Tagged extends Value {
	@XmlAttribute
	public String tagName;
	@XmlElement(name="element")
	public Object[] elements = new Object[]{};
	@XmlAttribute
	public int size;
	public Tagged() {}
	public Tagged(String tagName, Object[] elements) {
		this.tagName = tagName;
		this.size = elements.length;
		this.elements = elements;
	}
	public String toString() {
		return super.toString() + "(" + tagName + ", "
			+ Arrays.toString(elements) + ")";
	}
}
