package orc.ast.oil.xml;

import java.util.Arrays;
import java.util.LinkedList;

import javax.xml.bind.annotation.XmlElement;

public class Definitions extends Expression {
	@XmlElement(name="definition")
	public Definition[] definitions;
	@XmlElement(required=true)
	public Expression body;
	public Definitions() {}
	public Definitions(Definition[] definitions, Expression body) {
		this.definitions = definitions;
		this.body = body;
	}
	public String toString() {
		return super.toString() + "(" + Arrays.toString(definitions) + ", " + body + ")";
	}
	@Override
	public orc.ast.oil.Expr unmarshal() {
		LinkedList<orc.ast.oil.Def> defs
			= new LinkedList<orc.ast.oil.Def>();
		for (Definition d : definitions) {
			defs.add(d.unmarshal());
		}
		return new orc.ast.oil.Defs(defs, body.unmarshal());
	}
}
