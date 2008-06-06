package orc.orchard.interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

import orc.orchard.error.InvalidProgramException;

public interface CompilerService extends Remote {
	public Oil compile(String program) throws InvalidProgramException, RemoteException;
}