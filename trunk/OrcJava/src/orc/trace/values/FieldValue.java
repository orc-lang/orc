//
// FieldValue.java -- Java class FieldValue
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

public class FieldValue extends AbstractValue {
	public final String name;

	public FieldValue(final String name) {
		super();
		this.name = name;
	}

	public void prettyPrint(final Writer out, final int indent) throws IOException {
		out.write(name);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return name == null ? 0 : name.hashCode();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object that) {
		return that instanceof FieldValue && ((FieldValue) that).name.equals(name);
	}
}
