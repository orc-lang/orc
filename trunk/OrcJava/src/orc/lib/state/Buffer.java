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
import orc.runtime.values.ListValue;

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
	public Object evaluate(Args args) {
		return new BufferInstance();
	}
	
	
	protected class BufferInstance extends DotSite {

		private LinkedList<Object> localBuffer;
		private LinkedList<Token> pendingQueue;
		/**
		 * Once this becomes true, no new items may be put,
		 * and gets on an empty buffer die rather than blocking.
		 */
		private boolean closed = false;

		BufferInstance() {
			localBuffer = new LinkedList<Object>();
			pendingQueue = new LinkedList<Token>();
		}
		
		@Override
		protected void addMethods() {
			addMethod("get", new Site() {
				public void callSite(Args args, Token receiver) {
					if (localBuffer.isEmpty()) {
						if (closed) receiver.die();
						else pendingQueue.addLast(receiver);
					} else {
						// If there is an item available, pop it and return it.
						receiver.resume(localBuffer.removeFirst());
					}
				}
			});	
			addMethod("put", new Site() {
				@Override
				public void callSite(Args args, Token sender) throws TokenException {
					Object item = args.getArg(0);
					if (closed) {
						sender.die();
						return;
					}
					if (pendingQueue.isEmpty()) {
						// If there are no waiting callers, buffer this item.
						localBuffer.addLast(item);
					} else {
						// If there are callers waiting, give this item to the top caller.
						Token receiver = pendingQueue.removeFirst();
						receiver.resume(item);
					}
					// Since this is an asynchronous buffer, a put call always returns.
					sender.resume();
				}
			});
			addMethod("getnb", new Site() {
				@Override
				public void callSite(Args args, Token receiver) {
					if (localBuffer.isEmpty()) receiver.die();
					else receiver.resume(localBuffer.removeFirst());
				}
			});
			addMethod("getAll", new EvalSite() {
				@Override
				public Object evaluate(Args args) throws TokenException {
					Object out = ListValue.make(localBuffer);
					localBuffer.clear();
					return out;
				}
			});	
			addMethod("isClosed", new EvalSite() {
				@Override
				public Object evaluate(Args args) throws TokenException {
					return closed;
				}
			});	
			addMethod("close", new EvalSite() {
				@Override
				public Object evaluate(Args args) throws TokenException {
					closed = true;
					for (Token pending : pendingQueue) {
						pending.die();
					}
					return signal();
				}
			});	
		}
	}
}
