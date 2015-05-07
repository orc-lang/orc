package orc.lib.math;

import java.math.BigDecimal;
import java.math.BigInteger;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Args.NumericBinaryOperator;
import orc.runtime.sites.EvalSite;
import orc.type.Type;
import orc.type.structured.ArrowType;

public class Exponent extends EvalSite {
	private static final MyOperator op = new MyOperator();
	private static final class MyOperator implements NumericBinaryOperator<Number> {
		public Number apply(BigInteger a, BigInteger b) {
			return a.pow(b.intValue());
		}
		public Number apply(BigDecimal a, BigDecimal b) {
		
			try {
				// Arbitrary-precision exponentiation only works if the exponent is integral
				return a.pow(b.intValueExact());
			}
			catch (ArithmeticException e) {
				// If the exponent is fractional or out of range, just use native double exponentiation
				// This _can_ lose precision.
				return java.lang.Math.pow(a.doubleValue(), b.doubleValue());
			}
	
		}
		public Number apply(int a, int b) {
			return java.lang.Math.pow(a,b);
		}
		public Number apply(long a, long b) {
			return java.lang.Math.pow(a,b);
		}
		public Number apply(byte a, byte b) {
			return java.lang.Math.pow(a,b);
		}
		public Number apply(short a, short b) {
			return java.lang.Math.pow(a,b);
		}
		public Number apply(double a, double b) {
			return java.lang.Math.pow(a,b);
		}
		public Number apply(float a, float b) {
			return java.lang.Math.pow(a,b);
		}
	}
	@Override
	public Object evaluate(Args args) throws TokenException {
		return Args.applyNumericOperator(
				args.numberArg(0), args.numberArg(1),
				op);
	}
	
	public Type type() {
		return new ArrowType(Type.NUMBER, Type.NUMBER, Type.NUMBER);
	}
}
