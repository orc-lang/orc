package orc.runtime.values;

import java.util.Collection;
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
public abstract class ListValue<E> extends Value implements Iterable<E>, ListLike, Collection<E>, Eq { 
	public abstract List<E> enlist();
	
	public static <E> ListValue<E> make(E[] vs) {
		ListValue l = NilValue.singleton;
		for (int i = vs.length - 1; i >= 0; i--) {
			l = new ConsValue(vs[i], l);
		}
		return l;
	}
	
	public static <E> ListValue<E> make(List<E> vs) {
		ListValue l = NilValue.singleton;
		Iterator i = new ReverseListIterator<E>(vs);
		while (i.hasNext()) {
			l = new ConsValue(i.next(), l);
		}
		return l;
	}
	
	public Iterator<E> iterator() {
		return enlist().iterator();
	}
	
	@Override
	public <T> T accept(Visitor<T> visitor) {
		return visitor.visit(this);
	}
	
	public abstract void uncons(Token caller);
	public abstract void unnil(Token caller);
	
	public boolean add(E arg0) {
		throw new UnsupportedOperationException();
	}

	public boolean addAll(Collection<? extends E> arg0) {
		throw new UnsupportedOperationException();
	}

	public void clear() {
		throw new UnsupportedOperationException();
	}

	public boolean containsAll(Collection<?> arg0) {
		// FIXME: inefficient implementation
		for (Object x : arg0) {
			if (!contains(x)) return false;
		}
		return true;
	}

	public boolean remove(Object arg0) {
		throw new UnsupportedOperationException();
	}

	public boolean removeAll(Collection<?> arg0) {
		throw new UnsupportedOperationException();
	}

	public boolean retainAll(Collection<?> arg0) {
		throw new UnsupportedOperationException();
	}

	public Object[] toArray() {
		return enlist().toArray();
	}

	public <T> T[] toArray(T[] a) {
		return enlist().toArray(a);
	}

}
