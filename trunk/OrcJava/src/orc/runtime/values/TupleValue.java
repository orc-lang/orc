//
// TupleValue.java -- Java class TupleValue
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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import orc.error.runtime.JavaException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.DotSite;
import orc.runtime.sites.PartialSite;
import orc.runtime.sites.core.Equal;

/**
 * A tuple value container
 * @author wcook, quark
 */
public class TupleValue extends DotSite implements Iterable<Object>, Eq {
	public Object[] values;

	public TupleValue(final List<Object> values) {
		this.values = new Object[values.size()];
		this.values = values.toArray(this.values);
	}

	public TupleValue(final Object... values) {
		this.values = values;
	}

	@Override
	protected void addMembers() {
		addMember("fits", new PartialSite() {
			@Override
			public Object evaluate(final Args args) throws TokenException {
				return args.intArg(0) == values.length ? Value.signal() : null;
			}
		});
	}

	@Override
	protected void defaultTo(final Args args, final Token token) throws TokenException {
		try {
			token.resume(values[args.intArg(0)]);
		} catch (final ArrayIndexOutOfBoundsException e) {
			throw new JavaException(e);
		}
	}

	public Object at(final int i) {
		return values[i];
	}

	public int size() {
		return values.length;
	}

	@Override
	public String toString() {
		if (values.length == 0) {
			return "signal";
		}
		return format('(', values, ", ", ')');
	}

	public static String format(final char left, final Object[] items, final String sep, final char right) {
		final StringBuffer buf = new StringBuffer();
		buf.append(left);
		for (int i = 0; i < items.length; ++i) {
			if (i > 0) {
				buf.append(sep);
			}
			buf.append(String.valueOf(items[i]));
		}
		buf.append(right);
		return buf.toString();
	}

	public List<Object> asList() {
		return Arrays.asList(values);
	}

	public Iterator<Object> iterator() {
		return asList().iterator();
	}

	@Override
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}

	@Override
	public boolean equals(final Object that_) {
		if (that_ == null) {
			return false;
		}
		return eqTo(that_);
	}

	public boolean eqTo(final Object that_) {
		if (!(that_ instanceof TupleValue)) {
			return false;
		}
		final TupleValue that = (TupleValue) that_;
		if (that.values.length != this.values.length) {
			return false;
		}
		for (int i = 0; i < this.values.length; ++i) {
			if (!Equal.eq(this.values[i], that.values[i])) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(values);
	}
}
