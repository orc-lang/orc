package orc.orchard.oil;

import javax.xml.bind.annotation.XmlSeeAlso;

import orc.orchard.errors.InvalidOilException;

/**
 * Orc values. For convenience, this subsumes all publishable values,
 * even those without a compile-time representation. If you try to
 * unmarshal a value with no compile-time representation you will
 * get an InvalidOilException.
 * 
 * <p>
 * It's nice that by explicitly including these in the Oil format,
 * we allow for the future possibility of including them as compile-time
 * values, so we can do optimizations like constant folding.
 * 
 * @author quark
 */
@XmlSeeAlso(value={Constant.class, Field.class, Site.class, UnrepresentableValue.class, List.class, Option.class, Tuple.class})
public abstract class Value extends Argument {
	@Override
	public abstract orc.ast.oil.arg.Arg unmarshal() throws InvalidOilException;
}
