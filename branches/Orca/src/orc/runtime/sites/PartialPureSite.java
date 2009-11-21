/**
 * 
 */
package orc.runtime.sites;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.transaction.Transaction;
import orc.runtime.values.Value;

/**
 * Abstract class for sites with a partial and immediate semantics: evaluate as for a partial
 * immediate site (see PartialSite), but if the evaluation returns null, the site remains silent.
 * The site "if" is a good example.
 * 
 * Subclasses must implement the method evaluate, which takes an argument list and returns
 * a single value (possibly null).
 *
 * @author dkitchin
 *
 */
public abstract class PartialPureSite extends PartialSite {

	/*
	 * Forward all calls, regardless of whether they are transactional.
	 */
	public void callSite(Args args, Token caller, Transaction transaction) throws TokenException {
		callSite(args, caller);
	}

}
