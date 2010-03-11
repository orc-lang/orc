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

import java.io.IOException;
import java.io.StringReader;

import orc.error.runtime.JavaException;
import orc.error.runtime.TokenException;
import orc.parser.AbortParse;
import orc.parser.OrcLiteralParser;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.type.Type;
import orc.type.TypeVariable;
import orc.type.structured.ArrowType;
import xtc.parser.ParseException;
import xtc.parser.Result;

/**
 * Read an Orc literal from a string.
 * @author quark
 */
public class Read extends EvalSite {
	@Override
	public Object evaluate(final Args args) throws TokenException {
		try {
			final OrcLiteralParser parser = new OrcLiteralParser(new StringReader(args.stringArg(0)), "<input string>");
			final Result result = parser.pLiteralValue(0);
			return parser.value(result);
		} catch (final AbortParse e) {
			throw new JavaException(e);
		} catch (final ParseException e) {
			throw new JavaException(e);
		} catch (final IOException e) {
			// should be impossible
			throw new AssertionError(e);
		}
	}

	@Override
	public Type type() {
		final TypeVariable X = new TypeVariable(0);
		return new ArrowType(Type.STRING, X, 1);
	}
}
