package orc.orchard.oil;

import java.util.Arrays;
import java.util.LinkedList;

import javax.xml.bind.annotation.XmlElement;
import orc.orchard.errors.InvalidOilException;

public class Definitions extends Expression {
	@XmlElement(name="definition")
	public Definition[] definitions;
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
	public orc.ast.oil.Expr unmarshal() throws InvalidOilException {
		LinkedList<orc.ast.oil.Def> defs
			= new LinkedList<orc.ast.oil.Def>();
		for (Definition d : definitions) {
			defs.add(d.unmarshal());
		}
		return new orc.ast.oil.Defs(defs, body.unmarshal());
	}
}
