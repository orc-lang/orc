//
// ListValue.java -- Java class ListValue
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

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

	public static <E> ListValue<E> make(final E[] vs) {
		ListValue l = NilValue.singleton;
		for (int i = vs.length - 1; i >= 0; i--) {
			l = new ConsValue(vs[i], l);
		}
		return l;
	}

	public static <E> ListValue<E> make(final List<E> vs) {
		ListValue l = NilValue.singleton;
		final Iterator i = new ReverseListIterator<E>(vs);
		while (i.hasNext()) {
			l = new ConsValue(i.next(), l);
		}
		return l;
	}

	public Iterator<E> iterator() {
		return enlist().iterator();
	}

	@Override
	public <T> T accept(final Visitor<T> visitor) {
		return visitor.visit(this);
	}

	public abstract void uncons(Token caller);

	public abstract void unnil(Token caller);

	public boolean add(final E arg0) {
		throw new UnsupportedOperationException();
	}

	public boolean addAll(final Collection<? extends E> arg0) {
		throw new UnsupportedOperationException();
	}

	public void clear() {
		throw new UnsupportedOperationException();
	}

	public boolean containsAll(final Collection<?> arg0) {
		// FIXME: inefficient implementation
		for (final Object x : arg0) {
			if (!contains(x)) {
				return false;
			}
		}
		return true;
	}

	public boolean remove(final Object arg0) {
		throw new UnsupportedOperationException();
	}

	public boolean removeAll(final Collection<?> arg0) {
		throw new UnsupportedOperationException();
	}

	public boolean retainAll(final Collection<?> arg0) {
		throw new UnsupportedOperationException();
	}

	public Object[] toArray() {
		return enlist().toArray();
	}

	public <T> T[] toArray(final T[] a) {
		return enlist().toArray(a);
	}

}
