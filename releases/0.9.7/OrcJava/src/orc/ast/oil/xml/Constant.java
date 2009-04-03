package orc.ast.oil.xml;

import javax.xml.bind.annotation.XmlElement;

import orc.ast.oil.xml.Argument;

public class Constant extends Argument {
	@XmlElement(required=true)
	public Object value;
	public Constant() {}
	public Constant(Object value) {
		this.value = value;
	}
	public String toString() {
		return super.toString() + "(" + value.getClass().toString() + "(" + value + "))";
	}
	@Override
	public orc.ast.oil.arg.Arg unmarshal() {
		return new orc.ast.oil.arg.Constant(value);
	}
}
