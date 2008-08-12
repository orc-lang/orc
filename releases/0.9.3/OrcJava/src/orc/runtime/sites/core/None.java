/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.sites.core;

import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.runtime.values.NoneValue;

/**
 * Implements the "none" option constructor site.
 * 
 * @author dkitchin
 */
public class None extends EvalSite {
	@Override
	public Object evaluate(Args args) {
		return new NoneValue();
	}
}
