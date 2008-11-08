package orc.lib.data;

import java.util.AbstractSet;
import java.util.HashSet;
import java.util.Iterator;

import orc.runtime.sites.core.Equal;

/**
 * Set which uses Orc's rules for equality.
 * 
 * @author quark
 */
public final class Set<E> extends AbstractSet<E> {
	private final HashSet<Wrapper<E>> set = new HashSet<Wrapper<E>>();
	
	private static class Wrapper<E> {
		public E value;
		public Wrapper(E value) {
			this.value = value;
		}
		public int hashCode() {
			return value.hashCode();
		}
		public boolean equals(Object that) {
			return Equal.eq(value, ((Wrapper<E>)that).value);
		}
	}
	
	@Override
	public boolean add(E arg0) {
		return set.add(new Wrapper<E>(arg0));
	}

	@Override
	public void clear() {
		set.clear();
	}

	@Override
	public boolean contains(Object arg0) {
		return set.contains(new Wrapper(arg0));
	}

	@Override
	public boolean isEmpty() {
		return set.isEmpty();
	}

	@Override
	public Iterator<E> iterator() {
		final Iterator<Wrapper<E>> iterator = set.iterator(); 
		return new Iterator<E>() {
			public boolean hasNext() {
				return iterator.hasNext();
			}

			public E next() {
				return iterator.next().value;
			}

			public void remove() {
				iterator.remove();
			}
		};
	}

	@Override
	public boolean remove(Object arg0) {
		return set.remove(new Wrapper(arg0));
	}

	@Override
	public int size() {
		return set.size();
	}
}
