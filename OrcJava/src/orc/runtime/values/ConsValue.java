package orc.runtime.values;

import java.util.List;

import orc.runtime.Token;
import orc.runtime.sites.core.Equal;

public class ConsValue extends ListValue {

	public Object head;
	public ListValue tail;
	
	
	public ConsValue(Object h, ListValue t) {
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
	public List<Object> enlist() {
		List<Object> tl = tail.enlist();
		tl.add(0,head);
		return tl;
	}
	
	@Override
	public <E> E accept(Visitor<E> visitor) {
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
}
