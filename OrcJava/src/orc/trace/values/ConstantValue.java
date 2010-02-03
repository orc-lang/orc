//
// ConstantValue.java -- Java class ConstantValue
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
import java.io.Serializable;
import java.io.Writer;

/**
 * Constant (not just immutable, but atomic) value,
 * such as a String, Number, Boolean, Character, or null.
 * @author quark
 */
public class ConstantValue extends AbstractValue {
	public final Serializable constant;

	public ConstantValue(final Serializable constant) {
		super();
		this.constant = constant;
	}

	public void prettyPrint(final Writer out, final int indent) throws IOException {
		out.write(orc.runtime.values.Value.write(constant));
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return constant == null ? 0 : constant.hashCode();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object that) {
		if (that == null) {
			return false;
		}
		if (!(that instanceof ConstantValue)) {
			return false;
		}
		final ConstantValue cv = (ConstantValue) that;
		if (cv.constant == null) {
			return constant == null;
		}
		return cv.constant.equals(constant);
	}
}
