/**
 * 
 */
package orc.runtime.sites.core;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.PartialSite;
import orc.runtime.sites.Site;
import orc.runtime.values.NoneValue;
import orc.runtime.values.SomeValue;
import orc.runtime.values.Value;

/**
 * @author dkitchin
 *
 */
public class TryNil extends Site {
	@Override
	public void callSite(Args args, Token caller) throws TokenException {
		args.valArg(0).unnil(caller);
	}
}