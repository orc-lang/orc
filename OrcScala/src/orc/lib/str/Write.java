//
// Write.java -- Java class Write
// Project OrcScala
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.str;

import orc.error.runtime.TokenException;
import orc.types.Type;
import orc.values.sites.TypedSite;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.EvalSite;
import orc.values.sites.compatibility.Types;

/**
 * Convert an Orc literal to a String.
 * 
 * @author quark
 */
public class Write extends EvalSite implements TypedSite {

    @Override
    public Object evaluate(final Args args) throws TokenException {
        final Object v = args.getArg(0);
        return orc.values.Format.formatValue(v);
    }

    @Override
    public Type orcType() {
        return Types.function(Types.top(), Types.string());
    }
}
