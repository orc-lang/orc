package orc.lib.util;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;

/**
 * Generate random UUIDs.
 * @author quark
 */
public class UUID extends EvalSite {
	@Override
	public Object evaluate(Args args) throws TokenException {
		return java.util.UUID.randomUUID().toString();
	}
}
