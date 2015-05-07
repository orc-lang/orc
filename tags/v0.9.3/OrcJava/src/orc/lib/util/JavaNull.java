package orc.lib.util;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;

/**
 * Access to Java's "null" value for use with Java
 * objects being used as sites.
 * @author quark
 */
public class JavaNull extends EvalSite {
	@Override
	public Object evaluate(Args args) throws TokenException {
		return null;
	}
}
