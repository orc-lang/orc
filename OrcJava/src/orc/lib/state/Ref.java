/**
 * 
 */
package orc.lib.state;

import java.util.LinkedList;
import java.util.Queue;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.DotSite;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.Site;

/**
 * @author dkitchin
 *
 * Rewritable mutable reference.
 * The reference can be initialized with a value, or left initially empty. 
 * Read operations block if the reference is empty.
 * Write operatons always succeed.
 *
 */
public class Ref extends EvalSite {

	/* (non-Javadoc)
	 * @see orc.runtime.sites.Site#callSite(java.lang.Object[], orc.runtime.Token, orc.runtime.values.GroupCell, orc.runtime.OrcEngine)
	 */
	@Override
	public Object evaluate(Args args) {
		
		// If we were passed arguments, condense those arguments and store them in the ref as an initial value
		if (args.size() > 0) {
			return new RefInstance(args.condense());
		}
		// Otherwise, create an initially empty reference
		else {
			return new RefInstance();
		}
	}
	
	
	public static class RefInstance extends DotSite {

		private Queue<Token> readQueue;
		Object contents;

		public RefInstance() {
			this.contents = null;
			
			/* Note that the readQueue also signals whether the reference has been assigned.
			 * If it is non-null (as it is initially), the reference is unassigned.
			 * If it is null, the reference has been assigned.
			 * 
			 * This allows the reference to contain a null value if needed, and it also
			 * frees the memory associated with the read queue once the reference has been assigned.
			 */
			this.readQueue = new LinkedList<Token>();
		}
		
		/* Create the reference with an initial value already assigned.
		 * In this case, we don't need a reader queue.
		 */
		public RefInstance(Object initial) {
			this.contents = initial;
			this.readQueue = null;
		}
		
		@Override
		protected void addMembers() {
			addMember("read", new readMethod());	
			addMember("write", new writeMethod());
			addMember("readnb", new Site() {
				@Override
				public void callSite(Args args, Token caller) throws TokenException {
					if (readQueue != null) caller.die();
					else caller.resume(contents);
				}
			});
		}
		
		private class readMethod extends Site {
			@Override
			public void callSite(Args args, Token reader) {

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
			public void callSite(Args args, Token writer) throws TokenException {

				Object val = args.getArg(0);
				
				/* Set the contents of the ref */
				contents = val;
				
				/* If the read queue is not null, the ref has not yet been set. */
				if (readQueue != null) {
					
					/* Wake up all queued readers and report the written value to them. */
					for (Token reader : readQueue) {
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

		public String toString() {
			return "Ref(" + contents + ")";
		}
	}
	
}
