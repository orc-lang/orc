/**
 * 
 */
package orc.lib.state;

import java.util.LinkedList;

import orc.runtime.OrcEngine;
import orc.runtime.Token;
import orc.runtime.sites.DotSite;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.Site;
import orc.runtime.values.Constant;
import orc.runtime.values.GroupCell;
import orc.runtime.values.Tuple;
import orc.runtime.values.Value;

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
	public Value evaluate(Tuple args) {
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
	
	private class SyncChannelInstance extends DotSite {

		// Invariant: senderQueue is empty or receiverQueue is empty
		private LinkedList<SenderItem> senderQueue;
		private LinkedList<Token> receiverQueue;

		SyncChannelInstance() {
			senderQueue = new LinkedList<SenderItem>();
			receiverQueue = new LinkedList<Token>();
		}
		
		@Override
		protected void addMethods() {
			addMethod("get", new getMethod());	
			addMethod("put", new putMethod());
		}
		
		private class getMethod extends Site {
			@Override
			public void callSite(Tuple args, Token returnToken, GroupCell caller, OrcEngine engine) {

				// If there are no waiting senders, put this caller on the queue
				if (senderQueue.isEmpty()) {
					receiverQueue.addLast(returnToken);
				}
				// If there is a waiting sender, both sender and receiver return
				else {
					SenderItem si = senderQueue.removeFirst();
					
					returnToken.setResult(new Constant(si.sent));
					engine.activate(returnToken);
					
					engine.siteReturn("SyncChannel.put", si.sender, signal());
				}

			}
		}
		
		private class putMethod extends Site {
			@Override
			public void callSite(Tuple args, Token returnToken, GroupCell caller, OrcEngine engine) {

				Object item = args.getArg(0);
				
				// If there are no waiting receivers, put this sender on the queue
				if (receiverQueue.isEmpty()) {
					senderQueue.addLast(new SenderItem(returnToken, item));
				}
				
				// If there is a waiting receiver, both receiver and sender return
				else {
					Token receiver = receiverQueue.removeFirst();
					
					returnToken.setResult(signal());
					engine.activate(returnToken);
					
					engine.siteReturn("SyncChannel.get", receiver, new Constant(item));
				}
				
			}
		}

	}
	
}
