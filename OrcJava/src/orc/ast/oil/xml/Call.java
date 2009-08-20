package orc.ast.oil.xml;

import java.util.Arrays;
import java.util.LinkedList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import orc.Config;
import orc.error.compiletime.CompilationException;

public class Call extends Expression {
	@XmlElement(required=true)
	public Argument callee;
	@XmlElementWrapper(required=true)
	@XmlElement(name="argument")
	public Argument[] arguments;
	public Call() {}
	public Call(Argument callee, Argument[] arguments) {
		this.callee = callee;
		this.arguments = arguments;
	}
	public String toString() {
		return super.toString() + "(" + callee + ", " + Arrays.toString(arguments) + ")";
	}
	@Override
	public orc.ast.oil.expression.Expr unmarshal(Config config) throws CompilationException {
		LinkedList<orc.ast.oil.expression.argument.Arg> args
			= new LinkedList<orc.ast.oil.expression.argument.Arg>();
		for (Argument a : arguments) {
			args.add(a.unmarshal(config));
		}
		orc.ast.oil.expression.Expr out = new orc.ast.oil.expression.Call(callee.unmarshal(config), args);
		return out;
	}
}
