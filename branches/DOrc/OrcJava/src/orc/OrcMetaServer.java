package orc;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import orc.runtime.OrcEngine;

interface RemoteOrcMetaServer extends Remote {
	public RemoteOrcServer newServer() throws RemoteException;
}

/**
 * A meta-server, or server factory. This creates OrcServer objects
 * on the local computer which can be used remotely.
 * @author quark
 */
public class OrcMetaServer extends UnicastRemoteObject implements RemoteOrcMetaServer {
	private static final long serialVersionUID = 1L;
	boolean debugMode;
	public OrcMetaServer(String name, boolean debugMode) throws RemoteException, MalformedURLException {
		super();
		this.debugMode = debugMode;
		Naming.rebind(name, this);
		if (debugMode) System.out.println("Waiting for connections...");
	}
	public RemoteOrcServer newServer() throws RemoteException {
		if (debugMode) System.out.println("newServer called");
		OrcEngine engine = new OrcEngine();
		engine.debugMode = debugMode;
		// This thread will actually process Orc tokens,
		// which will be fed to it by the remote object.
		new Thread(engine).start();
		return new OrcServer(engine);
	}
	public static void main(String[] args) {
		String name = "orc";
		boolean debugMode = false;
		for (int i = 0; i < args.length; ++i) {
			if (args[i].equals("-debug")) {
				debugMode = true;
			} else {
				name = args[i];
			}
		}
		try {
			new OrcMetaServer(name, debugMode);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}