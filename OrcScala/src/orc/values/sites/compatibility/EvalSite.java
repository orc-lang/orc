//
// EvalSite.java -- Java class EvalSite
// Project OrcScala
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

/**
 * @author jthywiss
 */
public abstract class EvalSite extends SiteAdaptor {

    @Override
    public void callSite(final Args args, final Handle caller) throws TokenException {
        caller.publish(object2value(evaluate(args)));
    }

    public abstract Object evaluate(Args args) throws TokenException;
}
