/**
 * 
 */
package orc.lib.net;

import orc.runtime.sites.PartialSite;

/**
 * @author dkitchin, mbickford
 * 
 * Return the name of the local host. If the lookup fails, the site remains silent.
 *
 */
public class Localhost extends PartialSite {

	/* (non-Javadoc)
	 * @see orc.runtime.sites.EvalSite#evaluate(java.lang.Object[])
	 */
	@Override
	public Object evaluate(Object[] args) {
		try
		{
			java.net.InetAddress localMachine = java.net.InetAddress.getLocalHost();	
			return localMachine.getHostName();
		}
		catch(java.net.UnknownHostException e)
		{
			System.err.println("unknown host.");
			e.printStackTrace();
			return null;
		}
	}

}
