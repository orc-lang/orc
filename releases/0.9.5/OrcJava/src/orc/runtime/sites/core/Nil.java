/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.sites.core;

import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.runtime.values.NilValue;

/**
 * Implements the "cons" constructor site.
 * 
 * @author dkitchin
 */
public class Nil extends EvalSite {

	@Override
	public Object evaluate(Args args) {
		return NilValue.singleton;
	}

}
