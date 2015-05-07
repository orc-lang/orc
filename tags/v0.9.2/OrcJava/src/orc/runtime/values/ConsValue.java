package orc.runtime.values;

import java.util.List;

import orc.error.TokenException;

public class ConsValue extends ListValue {

	private Value h;
	private ListValue t;
	
	
	public ConsValue(Value h, ListValue t) {
		this.h = h;
		this.t = t;
	}

	public boolean isCons() { return true; }
	
	public Value head() { return h; }
	public ListValue tail() { return t; }
	
	
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
