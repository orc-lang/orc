/**
 * 
 */
package orc.runtime.sites;

import orc.runtime.OrcEngine;
import orc.runtime.Token;
import orc.runtime.values.GroupCell;

/**
 * Abstract class for sites with a total and immediate semantics: evaluate the arguments and
 * return a value without blocking and without affecting the Orc engine. Essentially, subclasses
 * of this class represent sites without any special concurrent behavior.
 * 
 * Subclasses must implement the method evaluate, which takes an argument list and returns
 * a single value.
 * 
 * TODO Make evaluate's return type more specific than Object.
 * 
 * @author dkitchin
 *
 */
public abstract class EvalSite extends Site {

	/* (non-Javadoc)
	 * @see orc.runtime.sites.Site#callSite(java.lang.Object[], orc.runtime.Token, orc.runtime.values.GroupCell, orc.runtime.OrcEngine)
	 */
	@Override
	public void callSite(Object[] args, Token returnToken, GroupCell caller,
			OrcEngine engine) {

		returnToken.setResult(evaluate(args));
		engine.activate(returnToken);
	}
	
	abstract public Object evaluate(Object[] args);

}
