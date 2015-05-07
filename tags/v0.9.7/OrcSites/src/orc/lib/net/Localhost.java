/**
 * 
 */
package orc.lib.net;

import java.net.UnknownHostException;

import orc.error.runtime.JavaException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;

/**
 * @author dkitchin, mbickford
 * 
 * Return the name of the local host. If the lookup fails, the site remains silent.
 *
 */
public class Localhost extends EvalSite {
	@Override
	public Object evaluate(Args args) throws TokenException {
		try {
			java.net.InetAddress localMachine = java.net.InetAddress.getLocalHost();	
			return localMachine.getHostName();
		} catch (UnknownHostException e) {
			throw new JavaException(e);
		}
	}

}
