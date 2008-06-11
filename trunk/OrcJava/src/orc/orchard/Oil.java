package orc.orchard;

import java.io.StringReader;

import javax.xml.bind.annotation.XmlTransient;

import orc.Config;
import orc.Orc;
import orc.ast.simple.Expression;

/**
 * JAXB does bad things if you extend another class
 * which is not specifically designed to be JAXB-marshalled.
 * So we can't inherit any implementation of this class, which
 * is OK since it's trivial anyways.
 * @author quark
 */
public class Oil implements orc.orchard.interfaces.Oil {
	private String program;

	public Oil() {}
	
	public Oil(String program) {
		this();
		setProgram(program);
	}
	
	public String getProgram() {
		return program;
	}

	public void setProgram(String program) {
		this.program = program;
	}

	public Expression getExpression() {
		return Orc.compile(new StringReader(program), new Config());
	}
}