/**
 * 
 */
package orc.lib.trans;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import orc.error.OrcError;
import orc.error.OrcException;
import orc.error.compiletime.typing.TypeException;
import orc.error.runtime.TokenException;
import orc.lib.state.types.BufferType;
import orc.lib.trans.FragileTransactionalBuffer.BufferInstance;
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
 * A choice guard for transactional guarded choice.
 * 
 */
public class GuardedChoice extends EvalSite {

	/* (non-Javadoc)
	 * @see orc.runtime.sites.Site#callSite(java.lang.Object[], orc.runtime.Token, orc.runtime.values.GroupCell, orc.runtime.OrcEngine)
	 */
	@Override
	public Object evaluate(Args args) {
		return new ChoiceInstance();
	}
	
	public Type type() throws TypeException {
		return new ArrowType(new ArrowType(Type.SIGNAL));
	}
	
	protected class ChoiceInstance extends TransactionalSite implements Cohort {

		private Transaction winner = null;
		
		public void callSite(Args args, Token caller, Transaction transaction) {
		
			// Register this site as a cohort if this call is transactional
			if (transaction != null) {
				transaction.addCohort(this);
			}
		
			caller.resume();
		}

		/* 
		 * Set the confirmed transaction as the winner.
		 */
		public void confirm(Token t, Transaction trans) {
			
			if (winner != null) {
				winner = trans;
			}
			else {
				throw new OrcError("Multiple confirms on choice; known bug in ready/confirm for GuardedChoice");
				// TODO: Fix this bug. If some transaction is currently in ready, other readying transactions
				// should suspend until the first confirms or rolls back.
			}
			
			// Note that we don't track which transactions are disjuncts for this choice;
			// they will 
			
			t.die();
		}

		/* 
		 * 
		 */
		public void ready(Token t, Transaction trans) {
			// If this choice has already been made, refuse other choices
			if (winner != null) {
				trans.abort();
			}
			t.die();
		}

		/* 
		 * Aborts are a no-op.
		 */
		public void rollback(Token t, Transaction trans) {
			t.die();
		}
				
	}

}

	



