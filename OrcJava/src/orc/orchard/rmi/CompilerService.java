package orc.orchard.rmi;

import java.io.InputStream;
import java.io.StringReader;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import orc.Config;
import orc.Orc;
import orc.orchard.error.InvalidProgramException;

/**
 * For now this only supports local usage.
 * @author quark
 *
 */
public class CompilerService implements
		orc.orchard.interfaces.CompilerService
{
	public Oil compile(String program) throws InvalidProgramException {
		return new Oil(program);
	}
}