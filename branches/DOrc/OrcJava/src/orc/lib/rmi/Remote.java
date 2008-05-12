package orc.lib.rmi;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import orc.error.OrcRuntimeTypeException;
import orc.runtime.Args;
import orc.runtime.sites.ThreadedSite;
import orc.runtime.sites.java.ThreadedObjectProxy;
import orc.runtime.values.Value;

/**
 * Create an object for a Java RMI service. 
 * @author adrian
 */
public class Remote extends ThreadedSite {
	@Override
	public Value evaluate(Args args) throws OrcRuntimeTypeException {
		try {
			return new orc.runtime.values.Site(
				new ThreadedObjectProxy(
					Naming.lookup(args.stringArg(0))));
		} catch (MalformedURLException e) {
			throw new OrcRuntimeTypeException(e);
		} catch (RemoteException e) {
			throw new OrcRuntimeTypeException(e);
		} catch (NotBoundException e) {
			throw new OrcRuntimeTypeException(e);
		}
	}
}