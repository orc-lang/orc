package orc.ast.xml.expression;

import java.util.Arrays;
import java.util.LinkedList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import orc.Config;
import orc.error.compiletime.CompilationException;

public class DeclareDefs extends Expression {
	@XmlElement(name="definition", required=true)
	public Def[] definitions;
	@XmlElement(required=true)
	public Expression body;
	public DeclareDefs() {}
	public DeclareDefs(Def[] definitions, Expression body) {
		this.definitions = definitions;
		this.body = body;
	}
	public String toString() {
		return super.toString() + "(" + Arrays.toString(definitions) + ", " + body + ")";
	}
	@Override
	public orc.ast.oil.expression.Expression unmarshal(Config config) throws CompilationException {
		LinkedList<orc.ast.oil.expression.Def> defs
			= new LinkedList<orc.ast.oil.expression.Def>();
		for (Def d : definitions) {
			defs.add(d.unmarshal(config));
		}
		return new orc.ast.oil.expression.DeclareDefs(defs, body.unmarshal(config));
	}
}
