package orc.runtime.values;

import java.util.List;

import orc.runtime.Token;
import orc.runtime.sites.core.Equal;

public class ConsValue<E> extends ListValue<E> {

	public E head;
	public ListValue<E> tail;
	
	
	public ConsValue(E h, ListValue<E> t) {
		this.head = h;
		this.tail = t;
	}

	@Override
	public void uncons(Token caller) {
		caller.resume(new TupleValue(this.head, this.tail));
	}
	
	@Override
	public void unnil(Token caller) {
		caller.die();
	}	
	
	public String toString() {
		return this.enlist().toString();
	}

	@Override
	public List<E> enlist() {
		List<E> tl = tail.enlist();
		tl.add(0,head);
		return tl;
	}
	
	@Override
	public <T> T accept(Visitor<T> visitor) {
		return visitor.visit(this);
	}
	
	@Override
	public boolean equals(Object that) {
		if (that == null) return false;
		return eqTo(that);
	}
	
	public boolean eqTo(Object that_) {
		if (!(that_ instanceof ConsValue)) return false;
		ConsValue that = (ConsValue)that_;
		return Equal.eq(head, that.head)
			&& tail.eqTo(that.tail);
	}
	
	@Override
	public int hashCode() {
		return head.hashCode() + 31 * tail.hashCode();
	}

	public boolean contains(Object o) {
		if (o == null) {
			return head == null || tail.contains(o);
		} else {
			return o.equals(head) || tail.contains(o);
		}
	}

	public boolean isEmpty() {
		return false;
	}

	public int size() {
		return 1 + tail.size();
	}
}
