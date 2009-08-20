package orc.ast.oil.xml;

import javax.xml.bind.annotation.XmlAttribute;

import orc.Config;
import orc.error.compiletime.CompilationException;

public class Field extends Argument {
	@XmlAttribute(required=true)
	public String name;
	public Field() {}
	public Field(String name) {
		this.name = name;
	}
	public String toString() {
		return super.toString() + "(" + name + ")";
	}
	@Override
	public orc.ast.oil.expression.argument.Arg unmarshal(Config config) throws CompilationException {
		return new orc.ast.oil.expression.argument.Field(name);
	}
}
