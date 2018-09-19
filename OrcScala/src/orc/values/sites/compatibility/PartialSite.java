//
// PartialSite.java -- Java class PartialSite
// Project OrcScala
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.values.sites.compatibility;

import orc.values.sites.compatibility.CallContext;
import orc.error.runtime.TokenException;

/**
 * Abstract class for sites with a partial and immediate semantics: evaluate as
 * for a total immediate site (see EvalSite), but if the evaluation returns
 * null, the site remains silent. The site "if" is a good example.
 * <p>
 * Subclasses must implement the method evaluate, which takes an argument list
 * and returns a single value (possibly null).
 *
 * @author dkitchin
 */
//@Deprecated
public abstract class PartialSite extends SiteAdaptor {

    @Override
    public void callSite(final Args args, final CallContext caller) throws TokenException {

        final Object v = evaluate(args);
        if (v != null) {
            caller.publish(object2value(v));
        } else {
            caller.halt();
        }

    }

    abstract public Object evaluate(Args args) throws TokenException;

}
