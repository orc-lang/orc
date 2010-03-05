//
// ValueMarshaller.java -- Java class ValueMarshaller
// Project Orchard
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.orchard.values;

import java.util.LinkedList;

import orc.runtime.values.Visitor;

public class ValueMarshaller extends Visitor<Object> {
	@Override
	public Object visit(final orc.runtime.values.ListValue v) {
		final java.util.List<Object> list = v.enlist();
		final java.util.List<Object> mlist = new LinkedList<Object>();
		for (final Object v2 : list) {
			mlist.add(visit(this, v2));
		}
		return new List(mlist.toArray(new Object[] {}));
	}

	@Override
	public Object visit(final orc.runtime.values.TupleValue v) {
		final Object mvalues[] = new Object[v.size()];
		int i = 0;
		for (final Object v2 : v) {
			mvalues[i++] = visit(this, v2);
		}
		return new Tuple(mvalues);
	}

	@Override
	public Object visit(final orc.runtime.values.TaggedValue v) {
		final Object mvalues[] = new Object[v.values.length];
		int i = 0;
		for (final Object v2 : v.values) {
			mvalues[i++] = visit(this, v2);
		}
		return new Tagged(v.tag.tagName, mvalues);
	}

	@Override
	public Object visit(final orc.runtime.values.Value v) {
		return new UnrepresentableValue(v.toString());
	}

	@Override
	public Object visit(final Object value) {
		if (value == null) {
			return value;
		} else if (value instanceof String) {
		} else if (value instanceof Boolean) {
		} else if (value instanceof Number) {
		} else if (value instanceof Character) {
		} else if (value instanceof java.util.Calendar) {
		} else if (value instanceof java.util.Date) {
		} else if (value instanceof javax.xml.namespace.QName) {
		} else if (value instanceof java.net.URI) {
		} else if (value instanceof javax.xml.datatype.Duration) {
		} else if (value instanceof java.util.UUID) {
		} else {
			return new UnrepresentableValue(value.toString());
		}
		return value;
	}
}
