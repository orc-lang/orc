package orc.runtime.values;

import java.util.LinkedList;
import java.util.List;

import orc.runtime.Token;

public class NilValue extends ListValue {
	public static final NilValue singleton = new NilValue();
	private NilValue() {}

	public String toString() { return "[]"; }
	
	@Override
	public void uncons(Token caller) {
		caller.die();
	}
	
	@Override
	public void unnil(Token caller) {
		caller.resume(Value.signal());
	}

	@Override
	public List<Object> enlist() {
		
		return new LinkedList<Object>();
	}
	
	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}	
}
