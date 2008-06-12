package orc.val;

import java.math.BigInteger;

import orc.ast.oil.arg.Arg;
import orc.ast.oil.arg.Constant;
import orc.env.Env;
import orc.runtime.values.Future;

public class Bool extends Val {

	public boolean b;
	
	public Bool(boolean b) {
		this.b = b;
	}

	@Override
	public Object toObject() {
		return b;
	}

	public String toString() { 
		return (b ? "true" : "false"); 
	}
}
