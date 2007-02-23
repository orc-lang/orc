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
import orc.runtime.values.GroupCell;

/**
 * @author cawellington, dkitchin
 *
 * Implements the local site Buffer.
 *
 */
public class Buffer extends EvalSite {

	/* (non-Javadoc)
	 * @see orc.runtime.sites.Site#callSite(java.lang.Object[], orc.runtime.Token, orc.runtime.values.GroupCell, orc.runtime.OrcEngine)
	 */
	@Override
	public Object evaluate(Object args[]) {
		return new BufferInstance();
	}
	
	
	private class BufferInstance extends DotSite {

		private LinkedList<Object> localBuffer;
		private LinkedList<Token> pendingQueue;

		BufferInstance() {
			localBuffer = new LinkedList<Object>();
			pendingQueue = new LinkedList<Token>();
		}
		
		@Override
		protected void addMethods() {
			addMethod("get", new getMethod());	
			addMethod("put", new putMethod());
		}
		
		private class getMethod extends Site {
			@Override
			public void callSite(Object[] args, Token returnToken, GroupCell caller, OrcEngine engine) {

				// If there are no buffered items, put this caller on the queue
				if (localBuffer.isEmpty()) {
					pendingQueue.addLast(returnToken);
				}
				// If there is an item available, pop it and return it.
				else {
					returnToken.setResult(localBuffer.removeFirst());
					engine.activate(returnToken);
				}

			}
		}
		
		private class putMethod extends Site {
			@Override
			public void callSite(Object[] args, Token returnToken, GroupCell caller, OrcEngine engine) {

				Object item = getArg(args,0);
				
				// If there are no waiting callers, buffer this item.
				if (pendingQueue.isEmpty()) {
					localBuffer.addLast(item);
				}
				// If there are callers waiting, give this item to the top caller.
				else {
					Token consumer = pendingQueue.removeFirst();
					consumer.setResult(item);
				}

				// Since this is an asynchronous buffer, a put call always returns.
				returnToken.setResult(signal());
				engine.activate(returnToken);
			}
		}

	}
	
}
