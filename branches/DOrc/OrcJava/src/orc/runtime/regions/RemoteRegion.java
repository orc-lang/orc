package orc.runtime.regions;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteRegion extends Remote {
	public void grow() throws RemoteException;
	public void shrink() throws RemoteException;
	public void close() throws RemoteException;
}
