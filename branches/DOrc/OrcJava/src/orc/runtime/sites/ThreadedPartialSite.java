package orc.runtime.sites;

import java.rmi.RemoteException;

import orc.error.OrcRuntimeTypeException;
import orc.runtime.Args;
import orc.runtime.RemoteToken;
import orc.runtime.values.Value;

/**
 * Abstract class for sites whose calls may block (the Java thread).
 * A separate thread is created for every call.
 * @author quark
 */
public abstract class ThreadedPartialSite extends Site {
	public void callSite(final Args args, final RemoteToken caller) throws OrcRuntimeTypeException {
		new Thread() {
			public void run() {
				try {
					try {
						Value value = evaluate(args);
						if (value == null) caller.die();
						else caller.resume(value);
					} catch (OrcRuntimeTypeException e) {
						caller.error(e);
					}
				} catch (RemoteException e1) {
					// TODO Auto-generated catch block
					throw new RuntimeException(e1);
				}
			}
		}.start();
	}
	abstract public Value evaluate(Args args) throws OrcRuntimeTypeException;
}