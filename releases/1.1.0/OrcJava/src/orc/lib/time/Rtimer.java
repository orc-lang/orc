//
// Rtimer.java -- Java class Rtimer
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

import java.util.TimerTask;

import orc.error.runtime.MessageNotUnderstoodException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.Site;
import orc.type.Type;
import orc.type.structured.ArrowType;
import orc.type.structured.DotType;

/**
 * Implements the RTimer site
 * @author wcook, quark, dkitchin
 */
public class Rtimer extends Site {
	@Override
	public void callSite(final Args args, final Token caller) throws TokenException {
		String f;
		try {
			f = args.fieldName();
		} catch (final TokenException e) {
			// default behavior is to wait
			caller.getEngine().scheduleTimer(new TimerTask() {
				@Override
				public void run() {
					caller.resume();
				}
			}, args.longArg(0));
			return;
		}
		if (f.equals("time")) {
			caller.resume(new EvalSite() {
				@Override
				public Object evaluate(final Args args) throws TokenException {
					return System.currentTimeMillis();
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
