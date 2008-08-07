package orc.ast.oil.arg;

import orc.ast.oil.Visitor;
import orc.env.Env;

public class Constant extends Arg { 
	
	public Object v;

	public Constant(Object v) {
		this.v = v;
	}

	@Override
	public Object resolve(Env<Object> env) {
		return v;
	}
	
	public String toString() {
		return "[" + v.toString() + "]";
	}
	
	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
