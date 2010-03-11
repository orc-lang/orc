//
// ReverseListIterator.java -- Java class ReverseListIterator
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

package orc.runtime;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * In Java 6 we can replace this with the builtin decreasingIterator.
 * @author quark
 */
public class ReverseListIterator<E> implements Iterator<E> {
	private final ListIterator<E> that;

	public ReverseListIterator(final List<E> list) {
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
