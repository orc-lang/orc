//
// LetType.java -- Java class LetType
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

import java.util.List;

import orc.error.compiletime.typing.TypeException;
import orc.type.Type;
import orc.type.structured.ArrowType;
import orc.type.structured.TupleType;

/**
 * The special type of the let site.
 * 
 * @author dkitchin
 */
public class LetType extends Type {

	// TODO: Add subtype, join, and meet relationships to Let

	@Override
	public Type call(final List<Type> args) throws TypeException {
		return condense(args);
	}

	/* By default, use the class name as the type's string representation */
	@Override
	public String toString() {
		return "let";
	}

	/**
	 * Classic 'let' functionality, at the type level. 
	 * Reduce a list of types into a single type as follows:
	 * 
	 * Zero arguments: return Top
	 * One argument: return that type
	 * Two or more arguments: return a tuple of the types
	 * 
	 */
	public static Type condense(final List<Type> types) {
		if (types.size() == 0) {
			return Type.SIGNAL;
		} else if (types.size() == 1) {
			return types.get(0);
		} else {
			return new TupleType(types);
		}
	}

	@Override
	public boolean subtype(final Type that) throws TypeException {
		if (that instanceof ArrowType) {
			final ArrowType arrow = (ArrowType) that;
			if (condense(arrow.argTypes).subtype(arrow.resultType)) {
				return true;
			}
		}

		return super.subtype(that);
	}

}
