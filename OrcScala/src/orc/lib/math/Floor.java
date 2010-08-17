//
// Floor.java -- Java class Floor
// Project OrcScala
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.math;

import orc.error.runtime.TokenException;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.EvalSite;
import orc.values.sites.compatibility.type.Type;
import orc.values.sites.compatibility.type.structured.ArrowType;

@SuppressWarnings("boxing")
public class Floor extends EvalSite {

	@Override
	public Object evaluate(final Args args) throws TokenException {
		final Number n = args.numberArg(0);
		return n.intValue();
	}

	@Override
	public Type type() {
		return new ArrowType(Type.NUMBER, Type.INTEGER);
	}
}
