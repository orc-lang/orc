//
// TryCons.java -- Java class TryCons
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
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.Site;
import orc.type.Type;
import orc.type.TypeVariable;
import orc.type.structured.ArrowType;
import orc.type.structured.ListType;
import orc.type.structured.TupleType;

/**
 * @author dkitchin
 *
 */
public class TryCons extends Site {
	@Override
	public void callSite(final Args args, final Token caller) throws TokenException {
		args.listLikeArg(0).uncons(caller);
	}

	@Override
	public Type type() throws TypeException {
		final Type X = new TypeVariable(0);
		final Type ListOfX = new ListType().instance(X);
		return new ArrowType(ListOfX, new TupleType(X, ListOfX), 1);
	}

}
