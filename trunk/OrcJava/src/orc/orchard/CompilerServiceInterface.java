package orc.orchard;

import java.rmi.Remote;
import java.rmi.RemoteException;


public interface CompilerServiceInterface {
	public Oil compile(String program) throws InvalidProgramException, RemoteException;
}