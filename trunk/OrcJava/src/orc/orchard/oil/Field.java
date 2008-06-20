package orc.orchard.oil;

import javax.xml.bind.annotation.XmlAttribute;

public class Field extends Value {
	@XmlAttribute
	public String name;
	public Field() {}
	public Field(String name) {
		this.name = name;
	}
	public String toString() {
		return super.toString() + "(" + name + ")";
	}
	@Override
	public orc.ast.oil.arg.Arg unmarshal() {
		return new orc.ast.oil.arg.Field(name);
	}
}
