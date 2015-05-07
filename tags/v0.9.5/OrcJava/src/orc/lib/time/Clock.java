/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.lib.time;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.DotSite;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.Site;
import orc.type.ArrowType;
import orc.type.EllipsisArrowType;
import orc.type.Type;

import java.util.Timer;
import java.util.TimerTask;

/**
 * A clock which measures the passage of real time.
 * @author quark
 */
public class Clock extends EvalSite {
	private static final long serialVersionUID = 1L;
	
	@Override
	public Object evaluate(Args args) throws TokenException {
		final long start = System.currentTimeMillis();
		return new EvalSite() {
			@Override
			public Object evaluate(Args args) throws TokenException {
				return System.currentTimeMillis() - start;
			}
		};
	}
}