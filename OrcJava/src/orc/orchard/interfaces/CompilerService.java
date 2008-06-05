package orc.orchard.interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface CompilerService extends Remote {
	public Oil compile(String program) throws RemoteException;
}