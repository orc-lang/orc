/**
 * 
 */
package orc.lib.comp;

import java.math.BigDecimal;
import java.math.BigInteger;

import orc.error.runtime.ArgumentTypeMismatchException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Args.NumericBinaryOperator;
import orc.runtime.sites.EvalSite;
import orc.type.Type;
import orc.type.structured.ArrowType;

/**
 * @author quark
 */
public abstract class ComparisonSite extends EvalSite {
	private static class MyOperator implements NumericBinaryOperator<Integer> {
		public Integer apply(BigInteger a, BigInteger b) {
			return a.compareTo(b);
		}
		public Integer apply(BigDecimal a, BigDecimal b) {
			return a.compareTo(b);
		}
		public Integer apply(int a, int b) {
			return a-b;
		}
		public Integer apply(long a, long b) {
			return (int)(a-b);
		}
		public Integer apply(byte a, byte b) {
			return a-b;
		}
		public Integer apply(short a, short b) {
			return a-b;
		}
		public Integer apply(double a, double b) {
			return Double.compare(a, b);
		}
		public Integer apply(float a, float b) {
			return Float.compare(a, b);
		}
	}
	
	/* (non-Javadoc)
	 * @see orc.runtime.sites.EvalSite#evaluate(java.lang.Object[])
	 */
	@Override
	public Object evaluate(Args args) throws TokenException {
		Object arg0 = args.getArg(0);
		Object arg1 = args.getArg(1);
		try {
			if (arg0 instanceof Number && arg1 instanceof Number) {
				int a = Args.applyNumericOperator(
						(Number)arg0, (Number)arg1,
						new MyOperator());
				return compare(a);
			} else {
				int a = ((Comparable)arg0).compareTo(arg1);
				return compare(a);
			}
		} catch (ClassCastException e) {
			throw new ArgumentTypeMismatchException(e);
		}
	}

	abstract public boolean compare(int a);
	
	public Type type() {
		return new ArrowType(Type.TOP, Type.TOP, Type.BOOLEAN);
	}
}
