//
// Datatype.java -- Java class Datatype
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
import orc.type.ground.DatatypeSiteType;

/**
 * For each string argument, creates a datatype constructor site; the string is
 * used as a label for printing and debugging. Returns these sites as a tuple.
 * 
 * @author dkitchin
 */
public class Datatype extends EvalSite {

	@Override
	public Object evaluate(final Args args) throws TokenException {

		final Object[] datasites = new Object[args.size()];

		for (int i = 0; i < datasites.length; i++) {

			final String label = args.stringArg(i);
			datasites[i] = new Datasite(label);
		}
		return Let.condense(datasites);
	}

	@Override
	public Type type() {
		return new DatatypeSiteType();
	}

}
