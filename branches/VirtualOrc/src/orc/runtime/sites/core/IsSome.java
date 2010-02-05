//
// IsSome.java -- Java class IsSome
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
import orc.type.structured.OptionType;

/**
 * @author dkitchin
 */
public class IsSome extends Site {
	@Override
	public void callSite(final Args args, final Token caller) throws TokenException {
		final Object result = Some.data.deconstruct(args.getArg(0));
		if (result == null) {
			caller.die();
		} else {
			caller.resume(result);
		}
	}

	@Override
	public Type type() throws TypeException {
		final Type X = new TypeVariable(0);
		final Type OptionX = new OptionType().instance(X);
		return new ArrowType(OptionX, X, 1);
	}

}
