package orc.lib.math;

import java.math.BigDecimal;
import java.math.BigInteger;

import orc.error.TokenException;
import orc.runtime.Args;
import orc.runtime.Args.NumericBinaryOperator;
import orc.runtime.sites.EvalSite;
import orc.runtime.values.Constant;
import orc.runtime.values.Value;

/**
 * NB: this is overloaded to operate on strings,
 * with implicit toString coercion (just like Java).
 * @author quark
 */
public class Add extends EvalSite {
	private static class MyOperator implements NumericBinaryOperator<Number> {
		public Number apply(BigInteger a, BigInteger b) {
			return a.add(b);
		}
		public Number apply(BigDecimal a, BigDecimal b) {
			return a.add(b);
		}
		public Number apply(int a, int b) {
			return a+b;
		}
		public Number apply(long a, long b) {
			return a+b;
		}
		public Number apply(byte a, byte b) {
			return a+b;
		}
		public Number apply(short a, short b) {
			return a+b;
		}
		public Number apply(double a, double b) {
			return a+b;
		}
		public Number apply(float a, float b) {
			return a+b;
		}
	}
	@Override
	public Value evaluate(Args args) throws TokenException {
		try {
    		return new Constant(Args.applyNumericOperator(
    				args.numberArg(0), args.numberArg(1),
    				new MyOperator()));
		} catch (TokenException _1) {
			// If the arguments aren't both numbers, maybe
			// one or the other is a string
			try {
				// the first argument is a string
    			String a = args.stringArg(0);
    			return new Constant(a + args.valArg(1).toString());
    		} catch (TokenException _2) {
				// the second argument is a string
    			String b = args.stringArg(1);
    			return new Constant(args.valArg(0).toString() + b);
    		}
		}
	}
}