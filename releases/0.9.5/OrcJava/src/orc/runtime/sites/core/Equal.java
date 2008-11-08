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
import orc.runtime.values.Eq;
import orc.type.ArrowType;
import orc.type.Type;

/**
 * @author dkitchin, quark
 */
public class Equal extends EvalSite {
	private static final NumericBinaryOperator<Boolean> op
	= new NumericBinaryOperator<Boolean>() {
		public Boolean apply(BigInteger a, BigInteger b) {
			return a.equals(b);
		}
		public Boolean apply(BigDecimal a, BigDecimal b) {
			return a.equals(b);
		}
		public Boolean apply(int a, int b) {
			return a == b;
		}
		public Boolean apply(long a, long b) {
			return a == b;
		}
		public Boolean apply(byte a, byte b) {
			return a == b;
		}
		public Boolean apply(short a, short b) {
			return a == b;
		}
		public Boolean apply(double a, double b) {
			return a == b;
		}
		public Boolean apply(float a, float b) {
			return a == b;
		}
	};
	
	/* (non-Javadoc)
	 * @see orc.runtime.sites.EvalSite#evaluate(java.lang.Object[])
	 */
	@Override
	public Object evaluate(Args args) throws TokenException {
		return eq(args.getArg(0), args.getArg(1));
	}
	
	/**
	 * Are two values equivalent, in the sense that one
	 * may be substituted for another without changing
	 * the meaning of the program?
	 * @see Eq
	 */
	public static boolean eq(Object a, Object b) {
		if (a == null || b == null) {
			return a == b;
		} else if (a instanceof Number && b instanceof Number) {
			try {
				// FIXME: should be a slightly more efficient way to do this
				return Args.applyNumericOperator((Number)a, (Number)b, op);
			} catch (TokenException e) {
				// should never happen
				throw new AssertionError(e);
			}
		} else if (a instanceof Character || b instanceof Character) {
			return a.equals(b);
		} else if (a instanceof Boolean || b instanceof Boolean) {
			return a.equals(b);
		} else if (a instanceof String || b instanceof String) {
			return a.equals(b);
		} else if (a instanceof Eq) {
			return ((Eq)a).eqTo(b);
		} else {
			return a == b;
		}
	}

	public static Type type() {
		return new ArrowType(Type.BOT, Type.BOT, Type.BOOLEAN);
	}
}
