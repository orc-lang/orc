//
// TupleValue.java -- Java class TupleValue
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2008 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.trace.values;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;

import orc.trace.Terms;

public class TupleValue extends AbstractValue {
	public final Value[] values;

	public TupleValue(final Value[] values) {
		super();
		this.values = values;
	}

	public void prettyPrint(final Writer out, final int indent) throws IOException {
		out.write("(");
		Terms.prettyPrintList(out, indent + 1, Arrays.asList(values), ", ");
		out.write(")");
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return Arrays.hashCode(values);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object value) {
		if (!(value instanceof TupleValue)) {
			return false;
		}
		final TupleValue that = (TupleValue) value;
		if (that.values.length != values.length) {
			return false;
		}
		for (int i = 0; i < values.length; ++i) {
			if (!values[i].equals(that.values[i])) {
				return false;
			}
		}
		return true;
	}
}
