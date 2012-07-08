//
// Eliza.java -- Java class Eliza
// Project OrcSites
//
// $Id$
//
// Copyright (c) 2012 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.fun;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import orc.Handle;
import orc.error.runtime.ArityMismatchException;
import orc.error.runtime.SiteException;
import orc.error.runtime.TokenException;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.DotSite;
import orc.values.sites.compatibility.EvalSite;

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
		return new DotSite() {
			@Override
			protected void addMembers() {
				addMember("finished", new EvalSite() {
					@Override
					public Object evaluate(final Args args) throws TokenException {
						if (args.size() != 0) { 
							throw new ArityMismatchException(1, args.size()); 
						}
						return eliza.finished();
					}
				});
			}

			@Override
			protected void defaultTo(final Args args, final Handle token) throws TokenException {
				if (args.size() != 1) { 
					throw new ArityMismatchException(1, args.size()); 
				}
				synchronized (eliza) {
					if (eliza.finished()) {
						token.halt();
					}
					try {
						token.publish(eliza.processInput(args.stringArg(0)));
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
