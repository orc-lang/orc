/**
 * 
 */
package orc.runtime.sites.core;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.Site;

/**
 * @author dkitchin
 *
 */
public class TryCons extends Site {
	@Override
	public void callSite(Args args, Token caller) throws TokenException {
		args.valArg(0).uncons(caller);
	}
}
