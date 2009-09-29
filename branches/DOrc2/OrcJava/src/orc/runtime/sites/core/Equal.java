/**
 * 
 */
package orc.runtime.sites.core;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashSet;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Args.NumericBinaryOperator;
import orc.runtime.sites.EvalSite;
import orc.runtime.values.Eq;
import orc.type.Type;
import orc.type.structured.ArrowType;

/**
 * Implement standard equality.
 * 
 * @author dkitchin, quark
 */
public class Equal extends EvalSite {
	private static HashSet<Class> valueClasses = new HashSet<Class>();
	static {
		synchronized (Equal.class) {
			valueClasses.add(Character.class);
			valueClasses.add(Boolean.class);
			valueClasses.add(String.class);
		}
	}
	
	/**
	 * Register a class as a "value class" which can be safely compared with
	 * {@link #equals(Object)}. Instances of value classes should be immutable.
	 * 
	 * <p>
	 * If possible, it's better if you make your class implement the interface
	 * {@link Eq} instead. This is here as a workaround for third-party
	 * libraries which can't be modified.
	 * 
	 * <p>
	 * You should call this from a static constructor, so you can be reasonably
	 * sure everything is registered before {@link Equal#eq(Object, Object)} is
	 * called.
	 * 
	 * @param c
	 */
	public static synchronized void registerValueClass(Class c) {
		valueClasses.add(c);
	}
	
	private static final NumericBinaryOperator<Boolean> op
	= new NumericBinaryOperator<Boolean>() {
		public Boolean apply(BigInteger a, BigInteger b) {
			return a.equals(b);
		}
		public Boolean apply(BigDecimal a, BigDecimal b) {
			return a.compareTo(b) == 0;
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
		// we have to handle Java immutable types specially;
		// for our own immutable types, we implement Eq;
		// for all other types, we use pointer equality
		if (a == null || b == null) {
			return a == b;
		} else if (a instanceof Eq) {
			return ((Eq)a).eqTo(b);
		} else if (a instanceof Number && b instanceof Number) {
			try {
				// FIXME: should be a slightly more efficient way to do this
				return Args.applyNumericOperator((Number)a, (Number)b, op);
			} catch (TokenException e) {
				// should never happen
				throw new AssertionError(e);
			}
		} else if (valueClasses.contains(a.getClass())) {
			return a.equals(b);
			// no need to check b, since both should be valueClasses to be equal
		} else {
			return a == b;
		}
	}

	public Type type() {
		return new ArrowType(Type.TOP, Type.TOP, Type.BOOLEAN);
	}
}
