/**
 * 
 */
package orc.lib.net;

import orc.runtime.Args;
import orc.runtime.sites.PartialSite;
import orc.runtime.values.Constant;
import orc.runtime.values.Value;

/**
 * @author dkitchin, mbickford
 * 
 * Return the name of the local host. If the lookup fails, the site remains silent.
 *
 */
public class Localhost extends PartialSite {

	@Override
	public Value evaluate(Args args) {
		try
		{
			java.net.InetAddress localMachine = java.net.InetAddress.getLocalHost();	
			return new Constant(localMachine.getHostName());
		}
		catch(java.net.UnknownHostException e)
		{
			System.err.println("unknown host.");
			e.printStackTrace();
			return null;
		}
	}

}
