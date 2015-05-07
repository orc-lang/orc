package orc.lib.math;

import java.math.BigDecimal;
import java.math.BigInteger;

import orc.error.TokenException;
import orc.runtime.Args;
import orc.runtime.Args.NumericBinaryOperator;
import orc.runtime.sites.EvalSite;
import orc.runtime.values.Constant;
import orc.runtime.values.Value;

public class Mult extends EvalSite {
	private static class MyOperator implements NumericBinaryOperator<Number> {
		public Number apply(BigInteger a, BigInteger b) {
			return a.multiply(b);
		}
		public Number apply(BigDecimal a, BigDecimal b) {
			return a.multiply(b);
		}
		public Number apply(int a, int b) {
			return a*b;
		}
		public Number apply(long a, long b) {
			return a*b;
		}
		public Number apply(byte a, byte b) {
			return a*b;
		}
		public Number apply(short a, short b) {
			return a*b;
		}
		public Number apply(double a, double b) {
			return a*b;
		}
		public Number apply(float a, float b) {
			return a*b;
		}
	}
	@Override
	public Value evaluate(Args args) throws TokenException {
		return new Constant(Args.applyNumericOperator(
				args.numberArg(0), args.numberArg(1),
				new MyOperator()));
	}
}
