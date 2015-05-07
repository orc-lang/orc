/**
 * 
 */
package orc.lib.math;

import java.math.BigDecimal;
import java.math.BigInteger;

import orc.error.TokenException;
import orc.runtime.Args;
import orc.runtime.Args.NumericUnaryOperator;
import orc.runtime.sites.EvalSite;
import orc.runtime.values.Constant;
import orc.runtime.values.Value;

/**
 * @author dkitchin
 *
 */
public class UMinus extends EvalSite {
	private static class MyOperator implements NumericUnaryOperator<Number> {
		public Number apply(BigInteger a) {
			return a.negate();
		}
		public Number apply(BigDecimal a) {
			return a.negate();
		}
		public Number apply(int a) {
			return -a;
		}
		public Number apply(long a) {
			return -a;
		}
		public Number apply(byte a) {
			return -a;
		}
		public Number apply(short a) {
			return -a;
		}
		public Number apply(double a) {
			return -a;
		}
		public Number apply(float a) {
			return -a;
		}
	}
	@Override
	public Value evaluate(Args args) throws TokenException {
		return new Constant(Args.applyNumericOperator(
				args.numberArg(0), new MyOperator()));
	}
}
