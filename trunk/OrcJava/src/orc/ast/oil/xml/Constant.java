package orc.ast.oil.xml;

import javax.xml.bind.annotation.XmlElement;

import orc.Config;
import orc.ast.oil.xml.Argument;
import orc.error.compiletime.CompilationException;

public class Constant extends Argument {
	@XmlElement(required=true, nillable=true)
	public Object value;
	public Constant() {}
	public Constant(Object value) {
		this.value = value;
	}
	public String toString() {
		return super.toString() + "(" + value.getClass().toString() + "(" + value + "))";
	}
	@Override
	public orc.ast.oil.expression.argument.Arg unmarshal(Config config) throws CompilationException {
		return new orc.ast.oil.expression.argument.Constant(value);
	}
}
