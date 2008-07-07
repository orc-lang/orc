package orc.orchard.oil;

import javax.xml.bind.annotation.XmlSeeAlso;
import orc.orchard.errors.InvalidOilException;

/**
 * Arguments to sites and expressions.
 * @author quark
 */
@XmlSeeAlso(value={Value.class, Variable.class})
public abstract class Argument extends Expression {
	@Override
	public abstract orc.ast.oil.arg.Arg unmarshal() throws InvalidOilException;
}
