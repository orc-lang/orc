package orc.lib.math;

import java.math.BigDecimal;
import java.math.BigInteger;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Args.NumericBinaryOperator;
import orc.runtime.sites.EvalSite;
import orc.type.Type;
import orc.type.structured.ArrowType;
import orc.type.structured.MultiType;

public class Sub extends EvalSite {
	private static final MyOperator op = new MyOperator();
	private static final class MyOperator implements NumericBinaryOperator<Number> {
		public Number apply(BigInteger a, BigInteger b) {
			return a.subtract(b);
		}
		public Number apply(BigDecimal a, BigDecimal b) {
			return a.subtract(b);
		}
		public Number apply(int a, int b) {
			return a-b;
		}
		public Number apply(long a, long b) {
			return a-b;
		}
		public Number apply(byte a, byte b) {
			return a-b;
		}
		public Number apply(short a, short b) {
			return a-b;
		}
		public Number apply(double a, double b) {
			return a-b;
		}
		public Number apply(float a, float b) {
			return a-b;
		}
	}
	@Override
	public Object evaluate(Args args) throws TokenException {
		return Args.applyNumericOperator(
				args.numberArg(0), args.numberArg(1),
				op);
	}
	
	public Type type() {
		return new MultiType(
				new ArrowType(Type.INTEGER, Type.INTEGER, Type.INTEGER),
				new ArrowType(Type.NUMBER, Type.NUMBER, Type.NUMBER)
				);
	}
}
