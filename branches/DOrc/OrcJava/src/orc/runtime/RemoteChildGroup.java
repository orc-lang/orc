package orc.runtime;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Remote interface to groups. See Group for more information.
 * @author quark
 */
public interface RemoteChildGroup extends Remote {
	public boolean isAlive() throws RemoteException;
	public void close() throws RemoteException;
}