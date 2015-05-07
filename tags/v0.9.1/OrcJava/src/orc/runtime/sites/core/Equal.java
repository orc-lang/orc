/**
 * 
 */
package orc.runtime.sites.core;

import java.math.BigDecimal;
import java.math.BigInteger;

import orc.error.TokenException;
import orc.runtime.Args;
import orc.runtime.Args.NumericBinaryOperator;
import orc.runtime.sites.EvalSite;
import orc.runtime.values.*;

/**
 * @author dkitchin, quark
 */
public class Equal extends EvalSite {
	private static class MyComparisonOperator implements NumericBinaryOperator<Boolean> {
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
	}
	
	/* (non-Javadoc)
	 * @see orc.runtime.sites.EvalSite#evaluate(java.lang.Object[])
	 */
	@Override
	public Value evaluate(Args args) throws TokenException {
		Object a = args.getArg(0);
		Object b = args.getArg(1);
		if (a instanceof Number && b instanceof Number) {
			return new Constant(Args.applyNumericOperator(
					(Number)a, (Number)b,
					new MyComparisonOperator()));
		}
		return new Constant(a.equals(b));	
	}
}
