/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.lib.time;

import orc.error.runtime.MessageNotUnderstoodException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.Site;
import orc.type.Type;
import orc.type.structured.ArrowType;
import orc.type.structured.DotType;
import orc.type.structured.EllipsisArrowType;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Implements the RTimer site
 * @author wcook, quark, dkitchin
 */
public class Rtimer extends Site {
	private static final long serialVersionUID = 1L;

	public void callSite(Args args, final Token caller) throws TokenException {
		String f;
		try {
			f = args.fieldName();
		} catch (TokenException e) {
			// default behavior is to wait
			caller.getEngine().scheduleTimer(new TimerTask() {
				public void run() {
					caller.resume();
				}
			}, args.longArg(0));	
			return;
		}
		if (f.equals("time")) {
			caller.resume(new EvalSite() {
				@Override
				public Object evaluate(Args args) throws TokenException {
					return System.currentTimeMillis();
				}
			});
		} else {
			throw new MessageNotUnderstoodException(f);
		}
	}
	
	public Type type() {
		return new DotType(new ArrowType(Type.NUMBER, Type.TOP))
			.addField("time", new ArrowType(Type.INTEGER));
	}
}