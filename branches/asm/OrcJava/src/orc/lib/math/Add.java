package orc.lib.math;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Args.NumericBinaryOperator;
import orc.runtime.sites.EvalSite;
import orc.type.ArrowType;
import orc.type.MultiType;
import orc.type.Type;

/**
 * NB: this is overloaded to operate on strings,
 * with implicit toString coercion (just like Java).
 * @author quark
 */
public class Add extends EvalSite {
	private static final MyOperator op = new MyOperator();
	private static final class MyOperator implements NumericBinaryOperator<Number> {
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
	public Object evaluate(Args args) throws TokenException {
		try {
    		return Args.applyNumericOperator(
    				args.numberArg(0), args.numberArg(1),
    				op);
		} catch (TokenException _1) {
			// If the arguments aren't both numbers, maybe
			// one or the other is a string
			try {
				// the first argument is a string
    			String a = args.stringArg(0);
    			return a + String.valueOf(args.getArg(1));
    		} catch (TokenException _2) {
				// the second argument is a string
    			String b = args.stringArg(1);
    			return String.valueOf(args.getArg(0)) + b;
    		}
		}
	}
	
	public Type type() {
		List<Type> alts = new LinkedList<Type>();
		
		alts.add(new ArrowType(Type.INTEGER, Type.INTEGER, Type.INTEGER));
		alts.add(new ArrowType(Type.NUMBER, Type.NUMBER, Type.NUMBER));
		alts.add(new ArrowType(Type.STRING, Type.TOP, Type.STRING));
		alts.add(new ArrowType(Type.TOP, Type.STRING, Type.STRING));
		
		return new MultiType(alts);
	}
}