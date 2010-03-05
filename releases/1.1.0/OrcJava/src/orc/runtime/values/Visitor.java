//
// Visitor.java -- Java class Visitor
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

import orc.runtime.sites.Site;

public abstract class Visitor<V> {
	public abstract V visit(Object value);

	public V visit(final Value value) {
		return this.visit((Object) value);
	}

	public V visit(final Closure v) {
		return this.visit((Value) v);
	}

	public V visit(final ListValue v) {
		return this.visit((Value) v);
	}

	public V visit(final NilValue v) {
		return this.visit((ListValue) v);
	}

	public V visit(final ConsValue v) {
		return this.visit((ListValue) v);
	}

	public V visit(final TupleValue v) {
		return this.visit((Value) v);
	}

	public V visit(final TaggedValue v) {
		return this.visit((Value) v);
	}

	public V visit(final Site v) {
		return this.visit((Value) v);
	}

	public V visit(final Field v) {
		return this.visit((Value) v);
	}

	public final static <V> V visit(final Visitor<V> visitor, final Object value) {
		if (value != null && value instanceof Value) {
			return ((Value) value).accept(visitor);
		} else {
			return visitor.visit(value);
		}
	}
}
