/**
 * 
 */
package orc.lib.state;

import java.util.LinkedList;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.DotSite;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.Site;

/**
 * @author dkitchin
 *
 * Implements the local site SyncChannel, which creates synchronous channels.
 *
 */
public class SyncChannel extends EvalSite {

	/* (non-Javadoc)
	 * @see orc.runtime.sites.Site#callSite(java.lang.Object[], orc.runtime.Token, orc.runtime.values.GroupCell, orc.runtime.OrcEngine)
	 */
	@Override
	public Object evaluate(Args args) {
		return new SyncChannelInstance();
	}
	
	
	private class SenderItem {
		
		Token sender;
		Object sent;
		
		SenderItem(Token sender, Object sent)
		{
			this.sender = sender;
			this.sent = sent;
		}
	}
	
	protected class SyncChannelInstance extends DotSite {

		// Invariant: senderQueue is empty or receiverQueue is empty
		private LinkedList<SenderItem> senderQueue;
		private LinkedList<Token> receiverQueue;

		SyncChannelInstance() {
			senderQueue = new LinkedList<SenderItem>();
			receiverQueue = new LinkedList<Token>();
		}
		
		@Override
		protected void addMembers() {
			addMember("get", new getMethod());	
			addMember("put", new putMethod());
		}
		
		private class getMethod extends Site {
			@Override
			public void callSite(Args args, Token receiver) {

				// If there are no waiting senders, put this caller on the queue
				if (senderQueue.isEmpty()) {
					receiverQueue.addLast(receiver);
				}
				// If there is a waiting sender, both sender and receiver return
				else {
					SenderItem si = senderQueue.removeFirst();
					Token sender = si.sender;
					Object item = si.sent;
					
					receiver.resume(item);
					sender.resume();
				}

			}
		}
		
		private class putMethod extends Site {
			@Override
			public void callSite(Args args, Token sender) throws TokenException {

				Object item = args.getArg(0);
				
				// If there are no waiting receivers, put this sender on the queue
				if (receiverQueue.isEmpty()) {
					senderQueue.addLast(new SenderItem(sender, item));
				}
				
				// If there is a waiting receiver, both receiver and sender return
				else {
					Token receiver = receiverQueue.removeFirst();
					
					receiver.resume(item);
					sender.resume();
				}
				
			}
		}

	}
	
}
