//
// ReadText.java -- Java class ReadText
// Project OrcScala
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.util;

import java.io.IOException;
import java.io.InputStreamReader;

import orc.error.runtime.ArgumentTypeMismatchException;
import orc.error.runtime.JavaException;
import orc.error.runtime.TokenException;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.EvalSite;

/**
 * Read an InputStreamReader into a String. Reads all remaining characters from
 * the stream, and closes it.
 *
 * @author quark
 */
public class ReadText extends EvalSite {
    @Override
    public Object evaluate(final Args args) throws TokenException {
        try {
            if (!(args.getArg(0) instanceof InputStreamReader)) {
                throw new ArgumentTypeMismatchException(0, "InputStreamReader", args.getArg(0).getClass().getCanonicalName());
            }
            final InputStreamReader in = (InputStreamReader) args.getArg(0);
            final StringBuilder out = new StringBuilder();
            final char[] buff = new char[1024];
            while (true) {
                final int blen = in.read(buff);
                if (blen < 0) {
                    break;
                }
                out.append(buff, 0, blen);
            }
            in.close();
            return out.toString();
        } catch (final IOException e) {
            throw new JavaException(e);
        }
    }
}
