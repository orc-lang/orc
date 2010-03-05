//
// Ltimer.java -- Java class Ltimer
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

package orc.lib.time;

import orc.error.runtime.MessageNotUnderstoodException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.regions.LogicalClock;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.Site;
import orc.type.Type;
import orc.type.structured.ArrowType;
import orc.type.structured.DotType;

/**
 * Site interface to the Orc engine's logical clock.
 */
public class Ltimer extends Site {
	@Override
	public void callSite(final Args args, final Token caller) throws TokenException {
		String f;
		try {
			f = args.fieldName();
		} catch (final TokenException e) {
			// default behavior is to wait
			caller.delay(args.intArg(0));
			return;
		}
		if (f.equals("time")) {
			// extract the clock, since it is ok to measure a clock 
			// from another thread
			final LogicalClock clock = caller.getLtimer();
			caller.resume(new EvalSite() {
				@Override
				public Object evaluate(final Args args) throws TokenException {
					return clock.getCurrentTime();
				}
			});
		} else {
			throw new MessageNotUnderstoodException(f);
		}
	}

	@Override
	public Type type() {
		return new DotType(new ArrowType(Type.NUMBER, Type.SIGNAL)).addField("time", new ArrowType(Type.INTEGER));
	}
}
