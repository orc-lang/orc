//
// Localhost.java -- Java class Localhost
// Project OrcSites
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.net;

import java.net.UnknownHostException;

import orc.error.runtime.JavaException;
import orc.error.runtime.TokenException;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.EvalSite;

/**
 * Return the name of the local host. If the lookup fails, the site remains
 * silent.
 *
 * @author dkitchin, mbickford
 */
public class Localhost extends EvalSite {
    @Override
    public Object evaluate(final Args args) throws TokenException {
        try {
            final java.net.InetAddress localMachine = java.net.InetAddress.getLocalHost();
            return localMachine.getHostName();
        } catch (final UnknownHostException e) {
            throw new JavaException(e);
        }
    }

}
