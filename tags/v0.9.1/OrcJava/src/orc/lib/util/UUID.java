package orc.lib.util;

import orc.error.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.runtime.values.Constant;
import orc.runtime.values.Value;

/**
 * Generate random UUIDs.
 * @author quark
 */
public class UUID extends EvalSite {
	@Override
	public Value evaluate(Args args) throws TokenException {
		return new Constant(java.util.UUID.randomUUID().toString());
	}
}
