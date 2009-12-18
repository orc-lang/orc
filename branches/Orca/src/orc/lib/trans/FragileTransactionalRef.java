/**
 * 
 */
package orc.lib.trans;

import java.util.LinkedList;
import java.util.Queue;

import orc.error.compiletime.typing.TypeException;
import orc.error.runtime.TokenException;
import orc.lib.state.types.BufferType;
import orc.lib.state.types.RefType;
import orc.lib.trans.FragileTransactionalBuffer.BufferInstance;
import orc.lib.trans.FragileTransactionalBuffer.BufferState;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.DotSite;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.Site;
import orc.runtime.sites.TransactionalSite;
import orc.runtime.transaction.Transaction;
import orc.type.Type;
import orc.type.TypeVariable;
import orc.type.structured.ArrowType;
import orc.type.structured.MultiType;

/**
 * @author dkitchin
 *
 * Eager-victim implementation of transactional rewritable mutable reference.
 * The reference can be initialized with a value, or left initially empty. 
 * Read operations block if the reference is empty.
 * Write operatons always succeed.
 *
 */
public class FragileTransactionalRef extends EvalSite {

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
	
	public Type type() throws TypeException {
		Type X = new TypeVariable(0);
		Type RefOfX = (new RefType()).instance(X);
		return new MultiType(new ArrowType(RefOfX, 1),
							 new ArrowType(X, RefOfX, 1));
	}
	
	
	public static class RefInstance extends DotSite {

		protected class RefState implements Versioned<RefState> {
			Queue<Token> readQueue;
			Object contents;
			
			public RefState(Object contents, Queue<Token> readQueue) {
				this.readQueue = readQueue;
				this.contents = contents;
			}

			/* (non-Javadoc)
			 * @see orc.lib.trans.Versioned#branch()
			 */
			public RefState branch() {
				// TODO Auto-generated method stub
				Queue<Token> newQueue = null;
				if (readQueue != null) {
					newQueue = new LinkedList<Token>();
					newQueue.addAll(readQueue);
				}
				return new RefState(contents, newQueue);
			}

			/* (non-Javadoc)
			 * @see orc.lib.trans.Versioned#merge(java.lang.Object)
			 */
			public void merge(RefState other) {
				this.readQueue = other.readQueue;
				this.contents = other.contents;
			}
		}
		
		FragileStateTree<RefState> stateTree;

		public RefInstance() {
			/* Note that the readQueue also signals whether the reference has been assigned.
			 * If it is non-null (as it is initially), the reference is unassigned.
			 * If it is null, the reference has been assigned.
			 * 
			 * This allows the reference to contain a null value if needed, and it also
			 * frees the memory associated with the read queue once the reference has been assigned.
			 */
			this(null, new LinkedList<Token>());
		}
		
		/* Create the reference with an initial value already assigned.
		 * In this case, we don't need a reader queue.
		 */
		public RefInstance(Object initial) {
			this(initial, null);
		}
		
		
		public RefInstance(Object initial, Queue<Token> readQueue) {
			this.stateTree = new FragileStateTree(new RefState(initial, readQueue));
		}
		
		@Override
		protected void addMembers() {
			addMember("read", new readMethod());	
			addMember("write", new writeMethod());
		}
		
		
		private class readMethod extends TransactionalSite {
			
			public void callSite(Args args, Token reader, Transaction transaction) throws TokenException {

				RefState state = stateTree.retrieve(transaction, this, args, reader);
				if (state == null) { return; }	
				
				
				/* If the read queue is not null, the ref has not been set.
				 * Add this caller to the read queue.
				 */ 
				if (state.readQueue != null) {
					state.readQueue.add(reader);
					reader.setQuiescent();
				}
				/* Otherwise, return the contents of the ref */
				else {
					reader.resume(state.contents);
				}
			}
		}
		
		private class writeMethod extends TransactionalSite {
			@Override
			public void callSite(Args args, Token writer, Transaction transaction) throws TokenException {

				RefState state = stateTree.retrieve(transaction, this, args, writer);
				if (state == null) { return; }	

				
				Object val = args.getArg(0);
				
				/* Set the contents of the ref */
				state.contents = val;
				
				/* If the read queue is not null, the ref has not yet been set. */
				if (state.readQueue != null) {
					
					/* Wake up all queued readers and report the written value to them. */
					for (Token reader : state.readQueue) {
						reader.unsetQuiescent();
						reader.resume(val);
					}
					
					/* Null out the read queue. 
					 * This indicates that the ref has been written.
					 * It also allowed the associated memory to be reclaimed.
					 */
					state.readQueue = null;
				}
				
				/* A write always succeeds and publishes a signal. */
				writer.resume();
			}
		}

		public String toString() {
			return "Ref(" + stateTree.getRoot().contents + ")";
		}
	}
	
}
