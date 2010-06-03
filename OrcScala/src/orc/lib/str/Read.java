//
// Read.java -- Java class Read
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
import orc.sites.compatibility.Args;
import orc.sites.compatibility.EvalSite;
import orc.sites.compatibility.type.Type;
import orc.sites.compatibility.type.TypeVariable;
import orc.sites.compatibility.type.structured.ArrowType;

/**
 * Read an Orc literal from a string.
 * @author quark
 */
public class Read extends EvalSite {
	@Override
	public Object evaluate(final Args args) throws TokenException {
//		try {
			return new Integer(123); // Fixme -- use Orc parser to interpret the input string as an Orc literal
//		} catch (final AbortParse e) {
//			throw new JavaException(e);
//		} catch (final ParseException e) {
//			throw new JavaException(e);
//		} catch (final IOException e) {
//			// should be impossible
//			throw new AssertionError(e);
//		}
	}

	@Override
	public Type type() {
		final TypeVariable X = new TypeVariable(0);
		return new ArrowType(Type.STRING, X, 1);
	}
}
