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

import orc.TokenAPI;
import orc.error.runtime.MessageNotUnderstoodException;
import orc.error.runtime.TokenException;
import orc.types.Type;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.EvalSite;
import orc.values.sites.compatibility.SiteAdaptor;

/**
 * Implements the RTimer site
 * @author wcook, quark, dkitchin
 */
public class Rtimer extends SiteAdaptor {
    private static java.util.Timer timer = new java.util.Timer(); 
	@Override
	public void callSite(final Args args, final TokenAPI caller) throws TokenException {
		String f;
		try {
			f = args.fieldName();
		} catch (final TokenException e) {
			// default behavior is to wait
		  Rtimer.timer.schedule(new TimerTask() {
				@Override
				public void run() {
					caller.publish(signal());
				}
			}, args.longArg(0));
			return;
		}
		if (f.equals("time")) {
			caller.publish(new EvalSite() {
				@Override
				public Object evaluate(final Args args) throws TokenException {
					return System.currentTimeMillis();
				}
			});
		} else {
			throw new MessageNotUnderstoodException(f);
		}
	}
}
