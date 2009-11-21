/**
 * 
 */
package orc.runtime.sites;

import orc.error.runtime.NontransactionalSiteException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.transaction.Transaction;

/**
 * A subclass of EvalSite which imposes the additional semantic condition that the
 * site's operations must have no side effects. 
 * 
 * Such sites do not enter transactions as cohorts, since their effect is the same
 * both inside and outside a transaction. 
 * 
 * Subclasses must implement the method evaluate, which takes an argument list and returns
 * a single value.
 * 
 * @author dkitchin
 *
 */
public abstract class PureSite extends EvalSite {
	
	/*
	 * Forward all calls, regardless of whether they are transactional.
	 */
	public void callSite(Args args, Token caller, Transaction transaction) throws TokenException {
		callSite(args, caller);
	}

}