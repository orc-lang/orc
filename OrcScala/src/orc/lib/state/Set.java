//
// Set.java -- Java class Set
// Project OrcScala
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.state;

import java.util.AbstractSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Set which uses Orc's rules for equality.
 * 
 * @author quark
 */
@SuppressWarnings({"hiding","boxing"})
public final class Set<E> extends AbstractSet<E> {
	private final java.util.Set<Wrapper<E>> set = Collections.synchronizedSet(new HashSet<Wrapper<E>>());

	private static class Wrapper<E> {
		public E value;

		public Wrapper(final E value) {
			this.value = value;
		}

		@Override
		public int hashCode() {
			return value.hashCode();
		}

        @Override
		public boolean equals(final Object that) {
			return value.equals(((Wrapper<?>) that).value);  //FIXME:Check if this is OK vs. using Eq site
		}
	}

	public class SetReference implements Reference<Boolean> {
		private final E key;

		public SetReference(final E key) {
			this.key = key;
		}

		@Override
		public Boolean read() {
			return contains(key);
		}

		@Override
		public void write(final Boolean value) {
			if (value) {
				add(key);
			} else {
				remove(key);
			}
		}
	}

	public Reference<Boolean> apply(final E key) {
		return new SetReference(key);
	}

	@Override
	public boolean add(final E arg0) {
		return set.add(new Wrapper<E>(arg0));
	}

	@Override
	public void clear() {
		set.clear();
	}

	@Override
	public boolean contains(final Object arg) {
		return set.contains(new Wrapper<Object>(arg));
	}

	@Override
	public boolean isEmpty() {
		return set.isEmpty();
	}

	@Override
	public Iterator<E> iterator() {
		final Iterator<Wrapper<E>> iterator = set.iterator();
		return new Iterator<E>() {
			@Override
			public boolean hasNext() {
				return iterator.hasNext();
			}

			@Override
			public E next() {
				return iterator.next().value;
			}

			@Override
			public void remove() {
				iterator.remove();
			}
		};
	}

	@Override
	public boolean remove(final Object arg0) {
		return set.remove(new Wrapper<Object>(arg0));
	}

	@Override
	public int size() {
		return set.size();
	}
}
