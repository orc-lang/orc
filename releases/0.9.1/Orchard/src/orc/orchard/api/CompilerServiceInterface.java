package orc.orchard.api;

import java.rmi.Remote;
import java.rmi.RemoteException;

import orc.orchard.errors.InvalidProgramException;
import orc.orchard.oil.Oil;

/**
 * Compile program text into OIL format.
 * @author quark
 */
public interface CompilerServiceInterface extends Remote {
	/**
	 * Compile program text.
	 * @return compiled program
	 * @throws InvalidProgramException in case of compilation error.
	 */
	public Oil compile(String devKey, String program) throws InvalidProgramException, RemoteException;
}