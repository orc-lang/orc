package orc.orchard.interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

import orc.orchard.error.InvalidProgramException;

public interface CompilerService<O extends Oil> extends Remote {
	public O compile(String program) throws InvalidProgramException, RemoteException;
}