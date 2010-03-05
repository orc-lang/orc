//
// ConsValue.java -- Java class ConsValue
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

import java.util.List;

import orc.runtime.Token;
import orc.runtime.sites.core.Equal;

public class ConsValue<E> extends ListValue<E> {

	public E head;
	public ListValue<E> tail;

	public ConsValue(final E h, final ListValue<E> t) {
		this.head = h;
		this.tail = t;
	}

	@Override
	public void uncons(final Token caller) {
		caller.resume(new TupleValue(this.head, this.tail));
	}

	@Override
	public void unnil(final Token caller) {
		caller.die();
	}

	@Override
	public String toString() {
		return this.enlist().toString();
	}

	@Override
	public List<E> enlist() {
		final List<E> tl = tail.enlist();
		tl.add(0, head);
		return tl;
	}

	@Override
	public <T> T accept(final Visitor<T> visitor) {
		return visitor.visit(this);
	}

	@Override
	public boolean equals(final Object that) {
		if (that == null) {
			return false;
		}
		return eqTo(that);
	}

	public boolean eqTo(final Object that_) {
		if (!(that_ instanceof ConsValue)) {
			return false;
		}
		final ConsValue that = (ConsValue) that_;
		return Equal.eq(head, that.head) && tail.eqTo(that.tail);
	}

	@Override
	public int hashCode() {
		return head.hashCode() + 31 * tail.hashCode();
	}

	public boolean contains(final Object o) {
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
