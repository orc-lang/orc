/**
 * 
 */
package orc.lib.state;

import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import orc.error.compiletime.typing.TypeException;
import orc.error.runtime.TokenException;
import orc.lib.state.types.BufferType;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.DotSite;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.Site;
import orc.runtime.sites.TransactionalSite;
import orc.runtime.transaction.Cohort;
import orc.runtime.transaction.Transaction;
import orc.runtime.values.ListValue;
import orc.type.Type;
import orc.type.TypeVariable;
import orc.type.structured.ArrowType;
import orc.type.structured.DotType;
import orc.type.tycon.MutableContainerType;

/**
 * @author dkitchin
 *
 * Eager-victim implementation of transactional asynchronous channels.
 * 
 *  
 * 
 *
 */
public class FragileTransactionalBuffer extends EvalSite {

	/* (non-Javadoc)
	 * @see orc.runtime.sites.Site#callSite(java.lang.Object[], orc.runtime.Token, orc.runtime.values.GroupCell, orc.runtime.OrcEngine)
	 */
	@Override
	public Object evaluate(Args args) {
		return new BufferInstance();
	}
	
	public Type type() throws TypeException {
		Type X = new TypeVariable(0);
		Type BufferOfX = (new BufferType()).instance(X);
		return new ArrowType(BufferOfX, 1);
	}
	
	
	class BufferState {
		
		public LinkedList<Object> buffer;
		public LinkedList<Token> readers;
		public TreeMap<Transaction, BufferState> children;
		public BufferState parent = null;
		public LinkedList<FrozenCall> frozenCalls;
		public Transaction frozenFor = null;
		
		public BufferState() {
			buffer = new LinkedList<Object>();
			readers = new LinkedList<Token>();
			children = new TreeMap<Transaction, BufferState>();
			frozenCalls = new LinkedList<FrozenCall>();
		}

		public BufferState(LinkedList<Object> buffer,
				LinkedList<Token> readers,
				TreeMap<Transaction, BufferState> children, BufferState parent,
				LinkedList<FrozenCall> frozenCalls, Transaction frozenFor) {
			this.buffer = buffer;
			this.readers = readers;
			this.children = children;
			this.parent = parent;
			this.frozenCalls = frozenCalls;
			this.frozenFor = frozenFor;
		}
		
		public void mergeFrom(BufferState other) {
			this.buffer = other.buffer;
			this.readers = other.readers;
		}
		
		public BufferState addDescendant(Transaction trans) {
			BufferState descendant = new BufferState();
			descendant.parent = this;
			descendant.buffer = (LinkedList<Object>)buffer.clone();
			descendant.readers = (LinkedList<Token>)readers.clone();
			
			children.put(trans, descendant);
			
			return descendant;
		}

		
		public void freeze(Transaction trans) {
			if (frozenFor == null) {
				frozenFor = trans;
			}
			else {
				/* A transaction readying to commit on top of another commit just gets booted.
				 * We assume that the current readying transaction will succeed, at which point
				 * this transaction will get aborted anyway.
				 */
				trans.abort();
			}
		}
		
		/**
		 * @return
		 */
		public boolean isFrozen() {
			return frozenFor != null;
		}

		/**
		 * @param transactionalSite
		 * @param args
		 * @param reader
		 * @param transaction
		 */
		public void addFrozenCall(Site site,
				Args args, Token reader, Transaction transaction) {
			frozenCalls.add(new FrozenCall(args, reader, transaction, site));
			
		}
		
		public void unfreeze() throws TokenException {
			for (FrozenCall c : frozenCalls) {
					c.unfreeze();
			}
			frozenCalls.clear();
			frozenFor = null;
		}
		
	}
	
	
	class FrozenCall {
		Args args;
		Token reader;
		Transaction transaction;
		Site site;
		
		public FrozenCall(Args args, Token reader, Transaction transaction,
				Site site) {
			this.args = args;
			this.reader = reader;
			this.transaction = transaction;
			this.site = site;
		}
		
		public void unfreeze() throws TokenException {
			site.callSite(args, reader, transaction);
		}
	}
	
	
	protected class BufferInstance extends DotSite implements Cohort {

		private BufferState root;

		BufferInstance() {
			root = new BufferState();
		}
		
		protected BufferState retrieveState(Transaction transaction) {
			if (transaction == null) {
				return root;
			}
			else {
				Transaction up = transaction.parent;
				BufferState above = retrieveState(up);
				
				BufferState here = above.children.get(transaction);
				if (here == null) {
					here = above.addDescendant(transaction);
				}
				return here;
			}			
		}
		
		@Override
		protected void addMembers() {
			addMember("get", new TransactionalSite() {
				public void callSite(Args args, Token reader, Transaction transaction) {
					
					/* Find the state associated with this transaction */
					BufferState state = retrieveState(transaction);
					
					/* If the state is frozen, queue this call and return */
					if (state.isFrozen()) {
						state.addFrozenCall(this, args, reader, transaction);
						return;
					}
					
					/* This is an effectful operation, so abort all subtransactions. */
					for(Transaction subt : state.children.keySet()) {
						subt.abort();
					}
					state.children.clear();
					
					
					if (state.buffer.isEmpty()) {
						reader.setQuiescent();
						state.readers.addLast(reader);
					} else {
						// If there is an item available, pop it and return it.
						reader.resume(state.buffer.removeFirst());
					}
				}
			});	
			addMember("put", new TransactionalSite() {
				@Override
				public void callSite(Args args, Token writer, Transaction transaction) throws TokenException {
					Object item = args.getArg(0);
					
					/* Find the state associated with this transaction */
					BufferState state = retrieveState(transaction);
					
					/* This is an effectful operation, so abort all subtransactions. */
					for(Transaction subt : state.children.keySet()) {
						subt.abort();
					}
					state.children.clear();
					
					
					if (state.readers.isEmpty()) {
						// If there are no waiting callers, buffer this item.
						state.buffer.addLast(item);
					} else {
						// If there are callers waiting, give this item to the top caller.
						Token receiver = state.readers.removeFirst();
						receiver.unsetQuiescent();
						receiver.resume(item);
					}
					// Since this is an asynchronous buffer, a put call always returns.
					writer.resume();
				}
			});
		}
	
		public String toString() {
			return super.toString() + root.buffer.toString();
		}

		/* 
		 * Merge this transaction's state version with its parent.
		 */
		public void confirm(Token t, Transaction trans) {
			BufferState target = retrieveState(trans);
			target.parent.mergeFrom(target);
			target.parent.children.remove(trans);
			t.die();
		}

		/* 
		 * Freeze the target of the commit (the parent state version)
		 */
		public void ready(Token t, Transaction trans) {
			BufferState target = retrieveState(trans);
			target.parent.freeze(trans);
			t.die();
		}

		/* 
		 * Aborts are cheap; just remove this state version from the tree.
		 */
		public void rollback(Token t, Transaction trans) {
			BufferState victim = retrieveState(trans);
			victim.parent.children.remove(trans);
			t.die();
		}
				
	}
}

	



