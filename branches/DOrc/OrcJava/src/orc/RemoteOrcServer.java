package orc;

import java.rmi.Remote;
import java.rmi.RemoteException;

import orc.runtime.FrozenToken;

public interface RemoteOrcServer extends Remote {
	public void run(FrozenToken t) throws RemoteException;
}
