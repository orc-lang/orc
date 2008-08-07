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
		Object a = args.getArg(0);
		Object b = args.getArg(1);
		if (a instanceof Number && b instanceof Number) {
			return Args.applyNumericOperator((Number)a, (Number)b, op);
		}
		return a.equals(b);	
	}
}
