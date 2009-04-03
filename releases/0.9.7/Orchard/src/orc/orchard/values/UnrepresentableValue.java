package orc.orchard.values;

import javax.xml.bind.annotation.XmlAttribute;

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
}
