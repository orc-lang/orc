//
// Write.java -- Java class Write
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

package orc.lib.str;

import orc.error.runtime.TokenException;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.EvalSite;
import orc.values.sites.compatibility.type.Type;
import orc.values.sites.compatibility.type.structured.ArrowType;

/**
 * Convert an Orc literal to a String.
 * @author quark
 */
public class Write extends EvalSite {
  
	@Override
	public Object evaluate(final Args args) throws TokenException {
      Object v = args.getArg(0);
      return orc.values.Format.formatValue(v);
	}

	@Override
	public Type type() {
		return new ArrowType(Type.TOP, Type.STRING);
	}
}
