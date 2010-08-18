//
// Eliza.java -- Java class Eliza
// Project OrcSites
//
// $Id$
//
// Copyright (c) 2008 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.fun;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import orc.error.runtime.SiteException;
import orc.error.runtime.TokenException;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.EvalSite;
import orc.values.sites.compatibility.PartialSite;

public class Eliza extends EvalSite {
	@Override
	public Object evaluate(final Args args) throws TokenException {
		final net.chayden.eliza.Eliza eliza = new net.chayden.eliza.Eliza();
		String script;
		if (args.size() > 0) {
			script = "/" + args.stringArg(0);
		} else {
			script = "/net/chayden/eliza/eliza.script";
		}
		final InputStream stream = Eliza.class.getResourceAsStream(script);
		if (stream == null) {
			throw new ElizaException("Eliza script '" + script + "' not found.");
		}
		try {
			eliza.readScript(new InputStreamReader(stream));
		} catch (final IOException e) {
			throw new ElizaException("Error reading script '" + script + "': " + e.toString());
		}
		return new PartialSite() {
			@Override
			public Object evaluate(final Args args) throws TokenException {
				synchronized (eliza) {
					if (eliza.finished()) {
						return null;
					}
					try {
						return eliza.processInput(args.stringArg(0));
					} catch (final IOException e) {
						throw new ElizaException("Error processing script: " + e.toString());
					}
				}
			}
		};
	}

	public static class ElizaException extends SiteException {
		private static final long serialVersionUID = 410086571116992559L;

		public ElizaException(final String msg) {
			super(msg);
		}
	}
}
