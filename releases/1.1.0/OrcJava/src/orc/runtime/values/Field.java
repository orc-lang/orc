//
// Field.java -- Java class Field
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

import java.io.Serializable;

/**
 * Distinguished representation for field names.
 * @author quark
 */
public class Field extends Value implements Serializable, Eq {
	private final String key;

	public Field(final String key) {
		assert key != null;
		this.key = key;
	}

	public String getKey() {
		return key;
	}

	@Override
	public String toString() {
		return super.toString() + "(" + key + ")";
	}

	@Override
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}

	@Override
	public boolean equals(final Object that) {
		if (that == null) {
			return false;
		}
		return eqTo(that);
	}

	@Override
	public int hashCode() {
		return key.hashCode();
	}

	public boolean eqTo(final Object that) {
		return that instanceof Field && key.equals(((Field) that).key);
	}
}
