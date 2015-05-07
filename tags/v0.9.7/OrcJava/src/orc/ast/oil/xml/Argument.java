package orc.ast.oil.xml;

import javax.xml.bind.annotation.XmlSeeAlso;

/**
 * Arguments to sites and expressions.
 * @author quark
 */
@XmlSeeAlso(value={Constant.class, Field.class, Site.class, Variable.class})
public abstract class Argument extends Expression {
	@Override
	public abstract orc.ast.oil.arg.Arg unmarshal();
}
