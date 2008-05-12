package orc.runtime.values;

import java.util.List;

public class ConsValue extends ListValue {
	private static final long serialVersionUID = 1L;
	public Value h;
	public Value t;
	
	
	public ConsValue(Value h, Value t) {
		this.h = h;
		this.t = t;
	}

	public boolean isCons() { return true; }
	
	public Value head() { return h; }
	public Value tail() { return t; }
	
	
	public String toString() {
		return this.enlist().toString();
	}

	@Override
	public List<Value> enlist() {
		
		List<Value> tl = ((ListValue)t).enlist();
		tl.add(0,h);
		return tl;
	}
}
