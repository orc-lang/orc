//
// BoolBinopSite.java -- Java class BoolBinopSite
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

package orc.lib.bool;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.type.Type;
import orc.type.structured.ArrowType;

/**
 *
 * @author dkitchin
 */
public abstract class BoolBinopSite extends EvalSite {

	/* (non-Javadoc)
	 * @see orc.runtime.sites.EvalSite#evaluate(java.lang.Object[])
	 */
	@Override
	public Object evaluate(final Args args) throws TokenException {

		return compute(args.boolArg(0), args.boolArg(1));
	}

	abstract public boolean compute(boolean a, boolean b);

	@Override
	public Type type() {
		return new ArrowType(Type.BOOLEAN, Type.BOOLEAN, Type.BOOLEAN);
	}

}
