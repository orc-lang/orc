package orc.lib.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import orc.error.JavaException;
import orc.error.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.ThreadedPartialSite;
import orc.runtime.values.Constant;
import orc.runtime.values.Value;

/**
 * Implement ping using {@link InetAddress#isReachable(int)}. Accepts the host
 * as a string and an optional timeout (defaulting to 10 seconds), and returns
 * the approximate time in milliseconds required to receive a response. If no
 * response is received within the timeout, does not publish.
 * 
 * <p>WARNING: if ICMP cannot be used for some reason (e.g. you are running the
 * program as a non-root user on a Linux system), this will fall back to a regular
 * TCP/IP request to port 7 (echo), which often fails due to firewalls and the like. 
 * 
 * @author quark
 */
public class Ping extends ThreadedPartialSite {
	@Override
	public Value evaluate(Args args) throws TokenException {
		try {
			InetAddress host = InetAddress.getByName(args.stringArg(0));
			long start = System.currentTimeMillis();
			boolean reachable = host.isReachable(args.size() > 1
					? args.intArg(1)
					: 10000);
			if (!reachable) {
				System.err.println("Could not reach " + host.toString());
				return null;
			}
			return new Constant(System.currentTimeMillis() - start);
		} catch (UnknownHostException e) {
			throw new JavaException(e);
		} catch (IOException e) {
			throw new JavaException(e);
		}
	}
}
