package orc.lib.rmi;

import java.rmi.RemoteException;

import orc.OrcServer;
import orc.error.OrcRuntimeTypeException;
import orc.runtime.Args;
import orc.runtime.OrcEngine;
import orc.runtime.RemoteToken;
import orc.runtime.Token;
import orc.runtime.sites.PassedByValueSite;
import orc.runtime.sites.Site;
import orc.runtime.values.Constant;

/**
 * Return a reference to the local server which can be used
 * in distributed computations.
 * @author quark
 */
public class LocalOrcServer extends Site implements PassedByValueSite {
	public void callSite(Args args, RemoteToken caller) throws OrcRuntimeTypeException, RemoteException {
		OrcEngine engine = ((Token)caller).getEngine();
		caller.resume(new Constant(new OrcServer(engine)));
	}
}