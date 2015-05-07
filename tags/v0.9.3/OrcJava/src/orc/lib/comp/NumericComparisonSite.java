/**
 * 
 */
package orc.lib.comp;

import java.math.BigDecimal;
import java.math.BigInteger;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Args.NumericBinaryOperator;
import orc.runtime.sites.EvalSite;

/**
 * @author quark
 */
public abstract class NumericComparisonSite extends EvalSite {
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
	public Object evaluate(Args args) throws TokenException{
		int a = Args.applyNumericOperator(
				args.numberArg(0), args.numberArg(1),
				new MyOperator());
		int b = 0;
		return compare(a, b);
	}

	abstract public boolean compare(int a, int b);
}
