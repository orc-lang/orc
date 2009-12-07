package orc.lib.math;

import java.math.BigDecimal;
import java.math.BigInteger;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Args.NumericBinaryOperator;
import orc.runtime.sites.EvalSite;
import orc.type.Type;
import orc.type.structured.ArrowType;

public class Ceil extends EvalSite {
	
	@Override
	public Object evaluate(Args args) throws TokenException {
		Number n = args.numberArg(0);
		int i = n.intValue();
		return (n.equals(i) ? i : i+1);
	}
	
	public Type type() {
		return new ArrowType(Type.NUMBER, Type.INTEGER);
	}
}
