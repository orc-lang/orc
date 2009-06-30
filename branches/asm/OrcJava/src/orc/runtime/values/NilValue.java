package orc.runtime.values;

import java.util.LinkedList;
import java.util.List;

import orc.runtime.Token;

public class NilValue<E> extends ListValue<E> {
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
	public List<E> enlist() {
		
		return new LinkedList<E>();
	}
	
	@Override
	public <T> T accept(Visitor<T> visitor) {
		return visitor.visit(this);
	}	
	
	public boolean eqTo(Object that) {
		return (that instanceof NilValue);
	}
	
	@Override
	public int hashCode() {
		return NilValue.class.hashCode();
	}

	public boolean contains(Object o) {
		return false;
	}

	public boolean isEmpty() {
		return true;
	}

	public int size() {
		return 0;
	}
}
