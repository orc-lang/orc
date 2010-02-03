//
// TaggedValue.java -- Java class TaggedValue
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

public class TaggedValue extends AbstractValue {
	// FIXME: we should serialize the tag id somehow
	public final String tagName;
	public final Value[] values;

	public TaggedValue(final String tagName, final Value[] values) {
		super();
		this.tagName = tagName;
		this.values = values;
	}

	public void prettyPrint(final Writer out, final int indent) throws IOException {
		out.write(tagName + "(");
		Terms.prettyPrintList(out, indent + 1, Arrays.asList(values), ", ");
		out.write(")");
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (tagName == null ? 0 : tagName.hashCode());
		result = prime * result + Arrays.hashCode(values);
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object value) {
		if (!(value instanceof TaggedValue)) {
			return false;
		}
		final TaggedValue that = (TaggedValue) value;
		if (!that.tagName.equals(tagName)) {
			return false;
		}
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
