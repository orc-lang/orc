//
// Inequal.java -- Java class Inequal
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
 * @author dkitchin, quark
 *
 */
public class Inequal extends EvalSite {
	@Override
	public Object evaluate(final Args args) throws TokenException {
		final Object a = args.getArg(0);
		final Object b = args.getArg(1);
		return !Equal.eq(a, b);
	}

	@Override
	public Type type() {
		return new ArrowType(Type.TOP, Type.TOP, Type.BOOLEAN);
	}
}
