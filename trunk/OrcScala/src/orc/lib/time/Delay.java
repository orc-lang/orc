//
// Delay.java -- Java class Delay
// Project OrcScala
//
// $Id$
//
// Created by amshali on Sep 14, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.time;

import java.util.TimerTask;

import orc.TokenAPI;
import orc.error.runtime.TokenException;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.EvalSite;
import orc.values.sites.compatibility.SiteAdaptor;
import orc.run.extensions.SupportForRtimer;

/**
 * This class is mainly designed to simulate a long 
 * running site with a Thread.sleep delay.
 * 
 *
 * @author amshali
 */
public class Delay extends SiteAdaptor {
    @Override
    public void callSite(final Args args, final TokenAPI caller) throws TokenException {
        String f = null;
        try {
            f = args.fieldName();
        } catch (final TokenException e) {
          try {
            Thread.currentThread().sleep(args.longArg(0));
          }
          catch(InterruptedException e2) {
            e2.printStackTrace();
          }
          caller.publish(signal());
        }
    }
}
