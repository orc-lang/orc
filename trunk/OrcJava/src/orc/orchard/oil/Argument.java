package orc.orchard.oil;

import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlTransient;

import orc.orchard.InvalidOilException;

@XmlSeeAlso(value={Constant.class, Field.class, Site.class, Variable.class})
public abstract class Argument extends Expression {
	@Override
	public abstract orc.ast.oil.arg.Arg unmarshal() throws InvalidOilException;
}
