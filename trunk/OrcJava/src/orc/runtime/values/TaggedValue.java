//
// TaggedValue.java -- Java class TaggedValue
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

import orc.runtime.sites.core.Datasite;
import orc.runtime.sites.core.Equal;

public class TaggedValue extends Value implements Eq {
	public Object[] values;
	public Datasite tag; // typically this is a reference to the injection site itself

	public TaggedValue(final Datasite tag, final Object[] values) {
		this.values = values;
		this.tag = tag;
	}

	@Override
	public String toString() {
		return tag.tagName + TupleValue.format('(', values, ",", ')');
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
		if (!(that_ instanceof TaggedValue)) {
			return false;
		}
		final TaggedValue that = (TaggedValue) that_;
		if (that.tag != this.tag) {
			return false;
		}
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
		return tag.hashCode() + 31 * Arrays.hashCode(values);
	}
}
