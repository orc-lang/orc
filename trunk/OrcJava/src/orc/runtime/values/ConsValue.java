package orc.runtime.values;

import java.util.List;

import orc.error.runtime.TokenException;
import orc.runtime.Token;

public class ConsValue extends ListValue {

	private Value h;
	private ListValue t;
	
	
	public ConsValue(Value h, ListValue t) {
		this.h = h;
		this.t = t;
	}

	@Override
	public void uncons(Token caller) {
		caller.resume(new TupleValue(this.h, this.t));
	}
	
	@Override
	public void unnil(Token caller) {
		caller.die();
	}	
	
	public String toString() {
		return this.enlist().toString();
	}

	@Override
	public List<Value> enlist() {
		List<Value> tl = t.enlist();
		tl.add(0,h);
		return tl;
	}
}
