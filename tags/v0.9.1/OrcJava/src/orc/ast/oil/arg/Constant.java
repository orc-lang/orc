package orc.ast.oil.arg;

import orc.ast.oil.Visitor;
import orc.env.Env;
import orc.runtime.values.Future;

public class Constant extends Arg { 
	
	public Object v;

	public Constant(Object v) {
		this.v = v;
	}

	@Override
	public Future resolve(Env<Future> env) {
		// TODO: Fix the runtime so it doesn't need this conversion anymore.
		return new orc.runtime.values.Constant(v);
	}
	
	public String toString() {
		return "[" + v.toString() + "]";
	}
	
	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
