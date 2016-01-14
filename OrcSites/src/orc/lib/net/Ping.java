//
// Ping.java -- Java class Ping
// Project OrcSites
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import orc.error.runtime.JavaException;
import orc.error.runtime.TokenException;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.PartialSite;

/**
 * Implement ping using {@link InetAddress#isReachable(int)}. Accepts the host
 * as a string and an optional timeout (defaulting to 10 seconds), and returns
 * the approximate time in milliseconds required to receive a response. If no
 * response is received within the timeout, does not publish.
 * <p>
 * WARNING: if ICMP cannot be used for some reason (e.g. you are running the
 * program as a non-root user on a Linux system), this will fall back to a
 * regular TCP/IP request to port 7 (echo), which often fails due to firewalls
 * and the like.
 *
 * @author quark
 */
public class Ping extends PartialSite {
    @Override
    public Object evaluate(final Args args) throws TokenException {
        try {
            final InetAddress host = InetAddress.getByName(args.stringArg(0));
            final long start = System.currentTimeMillis();
            final boolean reachable = host.isReachable(args.size() > 1 ? args.intArg(1) : 10000);
            if (!reachable) {
                System.err.println("Could not reach " + host.toString());
                return null;
            }
            return System.currentTimeMillis() - start;
        } catch (final UnknownHostException e) {
            throw new JavaException(e);
        } catch (final IOException e) {
            throw new JavaException(e);
        }
    }
}
