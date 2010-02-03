//
// URandom.java -- Java class URandom
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

package orc.lib.util;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.PartialSite;
import orc.type.Type;
import orc.type.structured.ArrowType;

public class URandom extends PartialSite {

	java.util.Random rnd;

	public URandom() {
		rnd = new java.util.Random();
	}

	@Override
	public Object evaluate(final Args args) throws TokenException {
		if (args.size() == 0) {
			return rnd.nextDouble();
		} else {
			return null;
		}
	}

	@Override
	public Type type() {
		return new ArrowType(Type.NUMBER);
	}

}
