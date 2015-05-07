package orc.orchard.oil;

import javax.xml.bind.annotation.XmlAttribute;

public class Variable extends Argument {
	@XmlAttribute
	public int index;
	public Variable() {}
	public Variable(int index) {
		this.index = index;
	}
	public String toString() {
		return super.toString() + "(" + index + ")";
	}
	@Override
	public orc.ast.oil.arg.Arg unmarshal() {
		return new orc.ast.oil.arg.Var(index);
	}
}
