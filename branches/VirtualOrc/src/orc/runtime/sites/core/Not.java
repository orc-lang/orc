//
// Not.java -- Java class Not
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

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.type.Type;
import orc.type.structured.ArrowType;

/**
 * @author dkitchin
 */
public class Not extends EvalSite {
	@Override
	public Object evaluate(final Args args) throws TokenException {
		return !args.boolArg(0);
	}

	@Override
	public Type type() {
		return new ArrowType(Type.BOOLEAN, Type.BOOLEAN);
	}
}
