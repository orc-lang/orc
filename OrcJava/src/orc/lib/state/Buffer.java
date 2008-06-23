/**
 * 
 */
package orc.lib.state;

import java.util.LinkedList;

import orc.error.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.DotSite;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.Site;
import orc.runtime.values.Value;

/**
 * @author cawellington, dkitchin
 *
 * Implements the local site Buffer, which creates buffers (asynchronous channels).
 *
 */
public class Buffer extends EvalSite {

	/* (non-Javadoc)
	 * @see orc.runtime.sites.Site#callSite(java.lang.Object[], orc.runtime.Token, orc.runtime.values.GroupCell, orc.runtime.OrcEngine)
	 */
	@Override
	public Value evaluate(Args args) {
		return new BufferInstance();
	}
	
	
	protected class BufferInstance extends DotSite {

		private LinkedList<Value> localBuffer;
		private LinkedList<Token> pendingQueue;

		BufferInstance() {
			localBuffer = new LinkedList<Value>();
			pendingQueue = new LinkedList<Token>();
		}
		
		@Override
		protected void addMethods() {
			addMethod("get", new getMethod());	
			addMethod("put", new putMethod());
		}
		
		private class getMethod extends Site {
			@Override
			public void callSite(Args args, Token receiver) {

				// If there are no buffered items, put this caller on the queue
				if (localBuffer.isEmpty()) {
					pendingQueue.addLast(receiver);
				}
				// If there is an item available, pop it and return it.
				else {
					receiver.resume(localBuffer.removeFirst());
				}

			}
		}
		
		private class putMethod extends Site {
			@Override
			public void callSite(Args args, Token sender) throws TokenException {

				Value item = args.valArg(0);
				
				// If there are no waiting callers, buffer this item.
				if (pendingQueue.isEmpty()) {
					localBuffer.addLast(item);
				}
				// If there are callers waiting, give this item to the top caller.
				else {
					Token receiver = pendingQueue.removeFirst();
					receiver.resume(item);
				}

				// Since this is an asynchronous buffer, a put call always returns.
				sender.resume();
			}
		}

	}
	
}
