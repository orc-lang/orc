//
// ConstIntType.java -- Java class ConstIntType
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

package orc.type.ground;

import orc.error.compiletime.typing.TypeException;
import orc.type.Type;

public class ConstIntType extends IntegerType {

	public Integer i;

	public ConstIntType(final Integer i) {
		this.i = i;
	}

	/* We use the Java inheritance hierarchy as a default */
	@Override
	public boolean subtype(final Type that) throws TypeException {

		if (that instanceof ConstIntType) {

			final ConstIntType other = (ConstIntType) that;

			return i == other.i;
		} else {
			return super.subtype(that);
		}
	}

	@Override
	public Type join(final Type that) throws TypeException {
		if (that instanceof ConstIntType) {
			final ConstIntType other = (ConstIntType) that;
			if (i == other.i) {
				return this;
			} else {
				return Type.INTEGER;
			}
		} else {
			return super.join(that);
		}

	}

	@Override
	public String toString() {
		return "integer(=" + i + ")";
	}

}
