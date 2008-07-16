package orc.ast.val;

import java.math.BigInteger;

import orc.ast.oil.arg.Arg;
import orc.ast.oil.arg.Constant;
import orc.env.Env;
import orc.runtime.values.Future;

public class Str extends Val {

	public String s;
	
	public Str(String s) {
		this.s = s;
	}

	@Override
	public Object toObject() {
		
		return s;
	}
	
	public String toString() { 
		return s; 
	}
}
