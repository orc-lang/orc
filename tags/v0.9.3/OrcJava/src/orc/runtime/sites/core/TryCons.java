/**
 * 
 */
package orc.runtime.sites.core;

import java.util.Arrays;
import java.util.Iterator;

import orc.error.runtime.ArgumentTypeMismatchException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.Site;
import orc.runtime.values.ListLike;
import orc.runtime.values.Value;

/**
 * @author dkitchin
 *
 */
public class TryCons extends Site {
	@Override
	public void callSite(Args args, Token caller) throws TokenException {
		try {
			args.listLikeArg(0).uncons(caller);
		} catch (ArgumentTypeMismatchException _) {
			caller.die();
		}
	}
}
