//
// Cat.java -- Java class Cat
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
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.type.Type;
import orc.type.structured.EllipsisArrowType;

/**
 * Note that you can also use the syntax "a" + "b" for string concatenation.
 * 
 * @author dkitchin
 */
public class Cat extends EvalSite {

	@Override
	public Object evaluate(final Args args) throws TokenException {

		final StringBuffer buf = new StringBuffer();

		for (int i = 0; i < args.size(); i++) {
			buf.append(String.valueOf(args.getArg(i)));
		}

		return buf.toString();
	}

	@Override
	public Type type() {
		return new EllipsisArrowType(Type.STRING, Type.STRING);
	}

}
