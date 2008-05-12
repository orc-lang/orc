package orc;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import orc.runtime.FrozenToken;
import orc.runtime.OrcEngine;

/**
 * Orc server. It's designed to receive tokens from other Orc processes via RMI.
 * @author quark
 */
public class OrcServer extends UnicastRemoteObject implements RemoteOrcServer {
	private static final long serialVersionUID = 1L;
	private OrcEngine engine;

	/**
	 * Attach to an existing engine.
	 * @param engine
	 * @throws RemoteException
	 */
	public OrcServer(OrcEngine engine) throws RemoteException {
		super();
		this.engine = engine;
	}

	public void run(final FrozenToken t) throws RemoteException {
		new Thread(new Runnable() {
			public void run() {
				t.thaw(engine);
			}
		}).start();
	}
}