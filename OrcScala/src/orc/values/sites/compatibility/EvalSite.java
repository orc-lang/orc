//
// EvalSite.java -- Java class EvalSite
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on Jun 2, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.values.sites.compatibility;

import orc.Handle;
import orc.error.runtime.TokenException;
import scala.Option;
import scala.Some;
import scala.Tuple2;

/**
 * 
 *
 * @author jthywiss
 */
public abstract class EvalSite extends SiteAdaptor {

	@Override
	public void callSite(final Args args, final Handle caller) throws TokenException {
		caller.publish(object2value(evaluate(args)));
	}

	public abstract Object evaluate(Args args) throws TokenException;

    @Override
    public boolean immediateHalt() { return true; }
    @Override
    public Tuple2<Object, Option<Object>> publications() { 
      return new Tuple2<Object, Option<Object>>(1, new Some<Object>(1));
    }
}
