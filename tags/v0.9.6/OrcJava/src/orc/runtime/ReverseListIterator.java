package orc.runtime;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * In Java 6 we can replace this with the builtin decreasingIterator.
 * @author quark
 */
public class ReverseListIterator<E> implements Iterator<E> {
	private ListIterator<E> that;
	public ReverseListIterator(List<E> list) {
		that = list.listIterator(list.size());
	}
	public boolean hasNext() {
		return that.hasPrevious();
	}

	public E next() {
		return that.previous();
	}

	public void remove() {
		that.remove();
	}
}