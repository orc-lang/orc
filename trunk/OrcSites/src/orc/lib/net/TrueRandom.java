//
// TrueRandom.java -- Java class TrueRandom
// Project OrcSites
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.net;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import orc.error.runtime.JavaException;
import orc.error.runtime.TokenException;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.EvalSite;

public class TrueRandom extends EvalSite {
	private static String baseURL = "http://www.random.org/integers/?num=1&col=1&base=10&format=plain&rnd=new";

	@Override
	public Object evaluate(final Args args) throws TokenException {
		try {
			final String number = HTTPUtils.getURL(new URL(baseURL + "&min=" + args.longArg(0) + "&max=" + (args.longArg(1) - 1)));
			return new Long(number.trim());
		} catch (final MalformedURLException e) {
			throw new JavaException(e);
		} catch (final IOException e) {
			throw new JavaException(e);
		}
	}
}
