package orc.lib.rmi;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.LinkedList;

import orc.error.OrcRuntimeTypeException;
import orc.runtime.Args;
import orc.runtime.sites.ThreadedSite;
import orc.runtime.sites.java.ThreadedObjectProxy;
import orc.runtime.values.Value;

/**
 * Synchronous buffer which may be shared between Orc engines.
 * Buffers with the same name share the same implementation.
 * @author adrian
 */
public class RemoteBuffer extends ThreadedSite {
	/**
	 * Remote interface for an asynchronous buffer.
	 */
	public static interface RemoteBufferIface<T> extends Remote {
		public void put(T o) throws RemoteException;
		public T get() throws RemoteException;
	}

	/**
	 * Remote implementation for an asynchronous buffer.
	 * 
	 * @author adrian
	 */
	public static class RemoteBufferImpl<T> extends UnicastRemoteObject
			implements RemoteBufferIface<T> {
		private static final long serialVersionUID = 1L;

		private LinkedList<T> localBuffer;

		public RemoteBufferImpl() throws RemoteException {
			super();
			localBuffer = new LinkedList<T>();
		}

		public synchronized T get() throws RemoteException {
			while (localBuffer.isEmpty()) {
				try {
					wait();
				} catch (InterruptedException e) {
					// continue
				}
			}
			return localBuffer.removeFirst();
		}

		public synchronized void put(T item) throws RemoteException {
			localBuffer.addLast(item);
			notify();
		}
	}

	@Override
	public Value evaluate(Args args) throws OrcRuntimeTypeException {
		try {
			String name = args.stringArg(0);
			// Try to create and bind a new buffer
			RemoteBufferIface buffer = new RemoteBufferImpl();
			Naming.rebind(name, buffer);
			return new orc.runtime.values.Site(new ThreadedObjectProxy(buffer));
		} catch (MalformedURLException e) {
			throw new OrcRuntimeTypeException(e);
		} catch (RemoteException e) {
			throw new OrcRuntimeTypeException(e);
		}
	}
}