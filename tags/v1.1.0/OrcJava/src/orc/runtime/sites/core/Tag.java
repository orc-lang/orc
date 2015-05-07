//
// Tag.java -- Java class Tag
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

/**
 *  Site for generating individual constructor sites for datatypes. Deprecated.
 * @deprecated
 */
@Deprecated
public class Tag extends EvalSite {

	@Override
	public Object evaluate(final Args args) throws TokenException {
		return new Datasite(args.stringArg(1));
	}

}
