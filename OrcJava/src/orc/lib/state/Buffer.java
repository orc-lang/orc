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
	public Value evaluate(Tuple args) {
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
			public void callSite(Tuple args, Token returnToken, GroupCell caller, OrcEngine engine) {

				// If there are no buffered items, put this caller on the queue
				if (localBuffer.isEmpty()) {
					pendingQueue.addLast(returnToken);
				}
				// If there is an item available, pop it and return it.
				else {
					returnToken.setResult(new Constant(localBuffer.removeFirst()));
					engine.activate(returnToken);
				}

			}
		}
		
		private class putMethod extends Site {
			@Override
			public void callSite(Tuple args, Token returnToken, GroupCell caller, OrcEngine engine) {

				Object item = args.getArg(0);
				
				// If there are no waiting callers, buffer this item.
				if (pendingQueue.isEmpty()) {
					localBuffer.addLast(item);
				}
				// If there are callers waiting, give this item to the top caller.
				else {
					Token consumer = pendingQueue.removeFirst();
					engine.siteReturn("Buffer.get", consumer, new Constant(item));
				}

				// Since this is an asynchronous buffer, a put call always returns.
				returnToken.setResult(signal());
				engine.activate(returnToken);
			}
		}

	}
	
}
