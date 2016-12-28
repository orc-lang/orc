//
// BoolBinopSite.java -- Java class BoolBinopSite
// Project OrcScala
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.bool;

import orc.error.runtime.TokenException;
import orc.types.Type;
import orc.values.sites.TypedSite;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.EvalSite;
import orc.values.sites.compatibility.Types;

/**
 * @author dkitchin
 */
public abstract class BoolBinopSite extends EvalSite implements TypedSite {

    @Override
    public Object evaluate(final Args args) throws TokenException {
        return new Boolean(compute(args.boolArg(0), args.boolArg(1)));
    }

    abstract public boolean compute(boolean a, boolean b);

    @Override
    public Type orcType() {
        return Types.function(Types.bool(), Types.bool(), Types.bool());
    }
	
    @Override
    public boolean nonBlocking() { return true; }
}
