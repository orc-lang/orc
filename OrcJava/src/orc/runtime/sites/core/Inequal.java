/**
 * 
 */
package orc.runtime.sites.core;

import java.math.BigDecimal;
import java.math.BigInteger;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Args.NumericBinaryOperator;
import orc.runtime.sites.EvalSite;
import orc.type.Type;
import orc.type.structured.ArrowType;

/**
 * @author dkitchin, quark
 *
 */
public class Inequal extends EvalSite {
	public Object evaluate(Args args) throws TokenException {
		Object a = args.getArg(0);
		Object b = args.getArg(1);
		return !Equal.eq(a, b);
	}
	
	public Type type() {
		return new ArrowType(Type.TOP, Type.TOP, Type.BOOLEAN);
	}
}
