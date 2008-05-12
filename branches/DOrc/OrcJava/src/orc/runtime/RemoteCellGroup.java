package orc.runtime;

import java.rmi.Remote;
import java.rmi.RemoteException;

import orc.runtime.values.FutureUnboundException;
import orc.runtime.values.RemoteGroupCell;
import orc.runtime.values.Value;

/**
 * Remote interface to groups. See Group for more information.
 * @author quark
 */
public interface RemoteCellGroup extends Remote {
	public Value getValue(RemoteGroupCell c) throws RemoteException, FutureUnboundException;
}