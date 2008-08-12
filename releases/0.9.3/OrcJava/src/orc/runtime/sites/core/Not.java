/**
 * 
 */
package orc.runtime.sites.core;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;

/**
 * @author dkitchin
 */
public class Not extends EvalSite {
	@Override
	public Object evaluate(Args args) throws TokenException {
		return !args.boolArg(0);
	}
}
