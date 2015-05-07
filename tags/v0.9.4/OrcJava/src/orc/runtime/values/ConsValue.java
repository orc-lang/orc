package orc.runtime.values;

import java.util.List;

import orc.runtime.Token;

public class ConsValue extends ListValue {

	public Object h;
	public ListValue t;
	
	
	public ConsValue(Object h, ListValue t) {
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
	public List<Object> enlist() {
		List<Object> tl = t.enlist();
		tl.add(0,h);
		return tl;
	}
	
	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
