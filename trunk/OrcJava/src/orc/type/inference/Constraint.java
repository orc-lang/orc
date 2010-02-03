//
// Constraint.java -- Java class Constraint
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

package orc.type.inference;

import orc.error.compiletime.typing.TypeException;
import orc.type.Type;
import orc.type.tycon.Variance;

/**
 * Upper and lower bound constraints on a type variable.
 * 
 * @author dkitchin
 */
public class Constraint {

	public Type lower = Type.BOT;
	public Type upper = Type.TOP;

	/* Add the constraint "T <: ..." */
	public void atLeast(final Type T) throws TypeException {
		lower = T.join(lower);
		if (!lower.subtype(upper)) {
			throw new TypeException("Could not infer type arguments; overconstrained. " + lower + " </: " + upper);
		}
	}

	/* Add the constraint "... <: T" */
	public void atMost(final Type T) throws TypeException {
		upper = T.meet(upper);
		if (!lower.subtype(upper)) {
			throw new TypeException("Could not infer type arguments; overconstrained. " + lower + " </: " + upper);
		}
	}

	/* Find the minimal type within these bounds under the given variance */
	public Type minimal(final Variance v) throws TypeException {

		return v.minimum(lower, upper);
	}

	@Override
	public String toString() {
		return lower + " <: ... <: " + upper;
	}
}
