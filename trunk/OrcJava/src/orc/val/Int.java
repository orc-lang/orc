package orc.val;

import java.math.BigInteger;

import orc.ast.oil.arg.Arg;
import orc.ast.oil.arg.Constant;
import orc.env.Env;
import orc.runtime.values.Future;

public class Int extends Val {

	public BigInteger i;

	public Int(Integer i) {
		this.i = new BigInteger(i.toString());
	}
	
	public Int(String s) {
		this.i = new BigInteger(s);
	}

	@Override
	public Object toObject() {
		return i.intValue();
	}
	
	public String toString() { 
		return i.toString(); 
	}
}
