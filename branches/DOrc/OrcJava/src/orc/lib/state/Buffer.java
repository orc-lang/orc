/**
 * 
 */
package orc.lib.state;

import java.rmi.RemoteException;
import java.util.LinkedList;

import orc.error.OrcRuntimeTypeException;
import orc.runtime.Args;
import orc.runtime.RemoteToken;
import orc.runtime.sites.DotSite;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.PassedByValueSite;
import orc.runtime.sites.Site;
import orc.runtime.values.Value;

/**
 * @author cawellington, dkitchin
 *
 * Implements the local site Buffer, which creates buffers (asynchronous channels).
 *
 * The buffer instances cannot be passed by value but the buffer factory can.
 */
public class Buffer extends EvalSite implements PassedByValueSite {

	/* (non-Javadoc)
	 * @see orc.runtime.sites.Site#callSite(java.lang.Object[], orc.runtime.Token, orc.runtime.values.GroupCell, orc.runtime.OrcEngine)
	 */
	@Override
	public Value evaluate(Args args) {
		return new orc.runtime.values.Site(new BufferInstance());
	}
	
	
	protected static class BufferInstance extends DotSite {

		private LinkedList<Value> localBuffer;
		private LinkedList<RemoteToken> pendingQueue;

		BufferInstance() {
			localBuffer = new LinkedList<Value>();
			pendingQueue = new LinkedList<RemoteToken>();
		}
		
		@Override
		protected void addMethods() {
			addMethod("get", new getMethod());	
			addMethod("put", new putMethod());
		}
		
		private class getMethod extends Site {
			@Override
			public void callSite(Args args, RemoteToken receiver) {

				// If there are no buffered items, put this caller on the queue
				if (localBuffer.isEmpty()) {
					pendingQueue.addLast(receiver);
				} else {
					// If there is an item available, pop it and return it.
					try {
						receiver.resume(localBuffer.removeFirst());
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						throw new RuntimeException(e);
					}
				}

			}
		}
		
		private class putMethod extends Site {
			@Override
			public void callSite(Args args, RemoteToken sender) throws OrcRuntimeTypeException {

				Value item = args.valArg(0);
				
				// If there are no waiting callers, buffer this item.
				if (pendingQueue.isEmpty()) {
					localBuffer.addLast(item);
				}
				// If there are callers waiting, give this item to the top caller.
				else {
					RemoteToken receiver = pendingQueue.removeFirst();
					try {
						receiver.resume(item);
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						throw new RuntimeException(e);
					}
				}

				// Since this is an asynchronous buffer, a put call always returns.
				try {
					sender.resume();
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					throw new RuntimeException(e);
				}
			}
		}

	}
}