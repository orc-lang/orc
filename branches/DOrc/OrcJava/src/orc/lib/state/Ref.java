/**
 * 
 */
package orc.lib.state;

import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.Queue;

import orc.error.OrcRuntimeTypeException;
import orc.runtime.Args;
import orc.runtime.RemoteToken;
import orc.runtime.sites.DotSite;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.PassedByValueSite;
import orc.runtime.sites.Site;
import orc.runtime.values.Value;

/**
 * @author dkitchin
 *
 * Rewritable mutable reference.
 * The reference can be initialized with a value, or left initially empty. 
 * Read operations block if the reference is empty.
 * Write operatons always succeed.
 *
 * While the actual ref cells cannot be passed by value, the factory can.
 */
public class Ref extends EvalSite implements PassedByValueSite {

	/* (non-Javadoc)
	 * @see orc.runtime.sites.Site#callSite(java.lang.Object[], orc.runtime.RemoteToken, orc.runtime.values.GroupCell, orc.runtime.OrcEngine)
	 */
	@Override
	public Value evaluate(Args args) {
		return new orc.runtime.values.Site(
				args.size() > 0
				? new RefInstance(args.condense())
				: new RefInstance());
	}
	
	
	protected static class RefInstance extends DotSite {

		private Queue<RemoteToken> readQueue;
		Value contents;

		RefInstance() {
			this.contents = null;
			
			/* Note that the readQueue also signals whether the reference has been assigned.
			 * If it is non-null (as it is initially), the reference is unassigned.
			 * If it is null, the reference has been assigned.
			 * 
			 * This allows the reference to contain a null value if needed, and it also
			 * frees the memory associated with the read queue once the reference has been assigned.
			 */
			this.readQueue = new LinkedList<RemoteToken>();
		}
		
		/* Create the reference with an initial value already assigned.
		 * In this case, we don't need a reader queue.
		 */
		RefInstance(Value initial) {
			this.contents = initial;
			this.readQueue = null;
		}
		
		@Override
		protected void addMethods() {
			addMethod("read", new readMethod());	
			addMethod("write", new writeMethod());
		}
		
		private class readMethod extends Site {
			@Override
			public void callSite(Args args, RemoteToken reader) throws RemoteException {

				/* If the read queue is not null, the ref has not been set.
				 * Add this caller to the read queue.
				 */ 
				if (readQueue != null) {
					readQueue.add(reader);
				}
				/* Otherwise, return the contents of the ref */
				else {
					reader.resume(contents);
				}
			}
		}
		
		private class writeMethod extends Site {
			@Override
			public void callSite(Args args, RemoteToken writer) throws OrcRuntimeTypeException, RemoteException {

				Value val = args.valArg(0);
				
				/* Set the contents of the ref */
				contents = val;
				
				/* If the read queue is not null, the ref has not yet been set. */
				if (readQueue != null) {
					
					/* Wake up all queued readers and report the written value to them. */
					for (RemoteToken reader : readQueue) {
						reader.resume(val);
					}
					
					/* Null out the read queue. 
					 * This indicates that the ref has been written.
					 * It also allowed the associated memory to be reclaimed.
					 */
					readQueue = null;
				}
				
				/* A write always succeeds and publishes a signal. */
				writer.resume();
			}
		}
	}	
}