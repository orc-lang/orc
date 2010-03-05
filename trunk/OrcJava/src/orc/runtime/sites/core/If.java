//
// If.java -- Java class If
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
import orc.runtime.sites.PartialSite;
import orc.runtime.values.Value;
import orc.type.Type;
import orc.type.structured.ArrowType;

/**
 * @author dkitchin
 *
 */
public class If extends PartialSite {

	@Override
	public Object evaluate(final Args args) throws TokenException {
		if (args.boolArg(0)) {
			return Value.signal();
		} else {
			return null;
		}
	}

	@Override
	public Type type() {
		return new ArrowType(Type.BOOLEAN, Type.SIGNAL);
	}

}
