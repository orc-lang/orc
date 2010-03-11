//
// Cons.java -- Java class Cons
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

package orc.runtime.sites.core;

import orc.error.compiletime.typing.TypeException;
import orc.error.runtime.ArgumentTypeMismatchException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.runtime.values.ConsValue;
import orc.runtime.values.ListValue;
import orc.type.Type;
import orc.type.TypeVariable;
import orc.type.structured.ArrowType;
import orc.type.structured.ListType;

/**
 * Implements the "cons" constructor site.
 * 
 * @author dkitchin
 */
public class Cons extends EvalSite {

	@Override
	public Object evaluate(final Args args) throws TokenException {
		final Object t = args.getArg(1);
		if (!(t instanceof ListValue)) {
			//throw new TokenException("Cons expects second argument to be a list value; got a value of type " + t.getClass().toString());
			throw new ArgumentTypeMismatchException(1, "List", t.getClass().toString());
		}
		return new ConsValue(args.getArg(0), (ListValue) t);
	}

	@Override
	public Type type() throws TypeException {
		final Type X = new TypeVariable(0);
		final Type ListOfX = new ListType().instance(X);
		return new ArrowType(X, ListOfX, ListOfX, 1);
	}

}
