package orc.ast.oil.xml;

import java.util.Arrays;
import java.util.LinkedList;

import javax.xml.bind.annotation.XmlElement;

public class Call extends Expression {
	@XmlElement(required=true)
	public Argument callee;
	@XmlElement(name="argument")
	public Argument[] arguments = new Argument[]{};
	public Call() {}
	public Call(Argument callee, Argument[] arguments) {
		this.callee = callee;
		this.arguments = arguments;
	}
	public String toString() {
		return super.toString() + "(" + callee + ", " + Arrays.toString(arguments) + ")";
	}
	@Override
	public orc.ast.oil.Expr unmarshal() {
		LinkedList<orc.ast.oil.arg.Arg> args
			= new LinkedList<orc.ast.oil.arg.Arg>();
		for (Argument a : arguments) {
			args.add(a.unmarshal());
		}
		orc.ast.oil.Expr out = new orc.ast.oil.Call(callee.unmarshal(), args);
		return out;
	}
}
