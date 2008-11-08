/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.sites.core;

import orc.error.runtime.RuntimeTypeException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.Site;
import orc.runtime.values.TupleValue;

/**
 * Implements the "some" option constructor site.
 * 
 * @author quark
 */
public class Some extends Site {
	// since tags are compared by object equality,
	// we need to share a tag amongst all instances of this site
	static final Datasite data = new Datasite(1, "Some");
	@Override
	public void callSite(Args args, Token caller) throws TokenException {
		data.callSite(args, caller);
	}
}
