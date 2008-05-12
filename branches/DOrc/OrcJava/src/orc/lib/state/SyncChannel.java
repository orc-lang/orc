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
import orc.runtime.values.Constant;
import orc.runtime.values.Value;

/**
 * @author dkitchin
 *
 * Implements the local site SyncChannel, which creates synchronous channels.
 *
 */
public class SyncChannel extends EvalSite implements PassedByValueSite {

	/* (non-Javadoc)
	 * @see orc.runtime.sites.Site#callSite(java.lang.Object[], orc.runtime.RemoteToken, orc.runtime.values.GroupCell, orc.runtime.OrcEngine)
	 */
	@Override
	public Value evaluate(Args args) {
		return new orc.runtime.values.Site(new SyncChannelInstance());
	}
	
	
	private static class SenderItem {
		
		RemoteToken sender;
		Value sent;
		
		SenderItem(RemoteToken sender, Value sent)
		{
			this.sender = sender;
			this.sent = sent;
		}
	}
	
	protected static class SyncChannelInstance extends DotSite {

		// Invariant: senderQueue is empty or receiverQueue is empty
		private LinkedList<SenderItem> senderQueue;
		private LinkedList<RemoteToken> receiverQueue;

		SyncChannelInstance() {
			senderQueue = new LinkedList<SenderItem>();
			receiverQueue = new LinkedList<RemoteToken>();
		}
		
		@Override
		protected void addMethods() {
			addMethod("get", new getMethod());	
			addMethod("put", new putMethod());
		}
		
		private class getMethod extends Site {
			@Override
			public void callSite(Args args, RemoteToken receiver) throws RemoteException {

				// If there are no waiting senders, put this caller on the queue
				if (senderQueue.isEmpty()) {
					receiverQueue.addLast(receiver);
				}
				// If there is a waiting sender, both sender and receiver return
				else {
					SenderItem si = senderQueue.removeFirst();
					RemoteToken sender = si.sender;
					Value item = si.sent;
					
					receiver.resume(new Constant(item));
					sender.resume();
				}

			}
		}
		
		private class putMethod extends Site {
			@Override
			public void callSite(Args args, RemoteToken sender) throws OrcRuntimeTypeException, RemoteException {

				Value item = args.valArg(0);
				
				// If there are no waiting receivers, put this sender on the queue
				if (receiverQueue.isEmpty()) {
					senderQueue.addLast(new SenderItem(sender, item));
				}
				
				// If there is a waiting receiver, both receiver and sender return
				else {
					RemoteToken receiver = receiverQueue.removeFirst();
					
					receiver.resume(new Constant(item));
					sender.resume();
				}
				
			}
		}

	}
	
}
