package orc.runtime.values;

import java.util.Iterator;
import java.util.List;

import orc.runtime.ReverseListIterator;
import orc.runtime.Token;

/**
 * Common ancestor for ConsValue and NilValue. Unlike scheme, the Cons
 * constructor does not allow you to create a degenerate cons where
 * the tail is not a list, so we can guarantee that all Conses actually
 * have a list structure. (If you want a degenerate cons, just use a
 * tuples.)
 */
public abstract class ListValue extends Value implements Iterable, ListLike { 
	
	public abstract List<Object> enlist();
	
	public static ListValue make(Object[] vs) {
		ListValue l = NilValue.singleton;
		for (int i = vs.length - 1; i >= 0; i--) {
			l = new ConsValue(vs[i], l);
		}
		return l;
	}
	
	public static ListValue make(List<Object> vs) {
		ListValue l = NilValue.singleton;
		Iterator i = new ReverseListIterator<Object>(vs);
		while (i.hasNext()) {
			l = new ConsValue(i.next(), l);
		}
		return l;
	}
	
	public Iterator iterator() {
		return enlist().iterator();
	}
	
	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
	
	public abstract void uncons(Token caller);
	public abstract void unnil(Token caller);
}
