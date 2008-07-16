package orc.ast.oil.arg;

import java.util.LinkedList;

import orc.ast.oil.Def;
import orc.ast.oil.Visitor;
import orc.ast.val.Int;
import orc.ast.val.Str;
import orc.ast.val.Val;
import orc.env.Env;
import orc.runtime.values.Future;

public class Constant extends Arg { 
	
	public Val v;

	public Constant(Val v) {
		this.v = v;
	}

	@Override
	public Future resolve(Env<Future> env) {
		// TODO: Fix the runtime so it doesn't need this conversion anymore.
		return new orc.runtime.values.Constant(v.toObject());
	}
	
	public String toString() {
		return "[" + v.toString() + "]";
	}
	
	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
