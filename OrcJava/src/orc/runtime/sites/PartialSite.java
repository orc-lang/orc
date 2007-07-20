/**
 * 
 */
package orc.runtime.sites;

import orc.runtime.OrcEngine;
import orc.runtime.Token;
import orc.runtime.values.GroupCell;
import orc.runtime.values.Tuple;
import orc.runtime.values.Value;

/**
 * Abstract class for sites with a partial and immediate semantics: evaluate as for a total
 * immediate site (see EvalSite), but if the evaluation returns null, the site remains silent.
 * The site "if" is a good example.
 * 
 * Subclasses must implement the method evaluate, which takes an argument list and returns
 * a single value (possibly null).
 * 
 * TODO Make evaluate's return type more specific than Object.
 * 
 * @author dkitchin
 *
 */
public abstract class PartialSite extends Site {

	/* (non-Javadoc)
	 * @see orc.runtime.sites.Site#callSite(java.lang.Object[], orc.runtime.Token, orc.runtime.values.GroupCell, orc.runtime.OrcEngine)
	 */
	@Override
	public void callSite(Tuple args, Token returnToken, GroupCell caller,
			OrcEngine engine) {
		Value res = evaluate(args);
		
		if (res != null)
		{
			returnToken.setResult(res);
			engine.activate(returnToken);
		}
	}
	
	abstract public Value evaluate(Tuple args);

}
