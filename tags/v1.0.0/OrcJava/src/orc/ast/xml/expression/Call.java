package orc.ast.xml.expression;

import java.util.Arrays;
import java.util.LinkedList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import orc.Config;
import orc.ast.xml.expression.argument.Argument;
import orc.ast.xml.type.Type;
import orc.error.compiletime.CompilationException;

public class Call extends Expression {
	@XmlElement(required=true)
	public Argument callee;
	@XmlElementWrapper(required=true)
	@XmlElement(name="argument")
	public Argument[] arguments;
	
	@XmlElementWrapper(required=false)
	@XmlElement(name="typeArgs")
	public Type[] typeArgs;
	
	public Call() {}
	public Call(Argument callee, Argument[] arguments, Type[] typeArgs) {
		this.callee = callee;
		this.arguments = arguments;
		this.typeArgs = typeArgs;
	}
	public String toString() {
		return super.toString() + "(" + callee + ", " + Arrays.toString(arguments) + ")";
	}
	@Override
	public orc.ast.oil.expression.Expression unmarshal(Config config) throws CompilationException {
		LinkedList<orc.ast.oil.expression.argument.Argument> args
			= new LinkedList<orc.ast.oil.expression.argument.Argument>();
		for (Argument a : arguments) {
			args.add(a.unmarshal(config));
		}
		orc.ast.oil.expression.Expression out = 
			new orc.ast.oil.expression.Call(callee.unmarshal(config), args, Type.unmarshalAll(typeArgs));
		return out;
	}
}
