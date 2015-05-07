package orc.lib.state;

import java.util.AbstractSet;
import java.util.HashSet;
import java.util.Iterator;

import orc.runtime.sites.core.Equal;
import orc.runtime.values.Reference;

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
	
	public class SetReference implements Reference<Boolean> {
		private E key;
		public SetReference(E key) {
			this.key = key;
		}
		public Boolean read() {
			return contains(key);
		}
		public void write(Boolean value) {
			if (value) add(key);
			else remove(key);
		}
	}
	
	@SuppressWarnings("unused")
	public Reference<Boolean> apply(final E key) {
		return new SetReference(key);
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
	public boolean contains(Object arg) {
		return set.contains(new Wrapper(arg));
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
