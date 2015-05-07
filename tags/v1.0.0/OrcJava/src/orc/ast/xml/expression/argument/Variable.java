package orc.ast.xml.expression.argument;

import javax.xml.bind.annotation.XmlAttribute;

import orc.Config;
import orc.error.compiletime.CompilationException;

public class Variable extends Argument {
	@XmlAttribute(required=true)
	public int index;
	public Variable() {}
	public Variable(int index) {
		this.index = index;
	}
	public String toString() {
		return super.toString() + "(" + index + ")";
	}
	@Override
	public orc.ast.oil.expression.argument.Argument unmarshal(Config config) throws CompilationException {
		return new orc.ast.oil.expression.argument.Variable(index);
	}
}
