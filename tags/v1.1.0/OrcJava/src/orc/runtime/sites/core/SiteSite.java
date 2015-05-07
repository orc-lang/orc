//
// SiteSite.java -- Java class SiteSite
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.runtime.sites.core;

import java.util.Arrays;
import java.util.List;

import orc.ast.oil.TokenContinuation;
import orc.ast.oil.expression.Stop;
import orc.error.compiletime.typing.ArgumentArityException;
import orc.error.compiletime.typing.TypeException;
import orc.error.runtime.SiteException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.regions.Region;
import orc.runtime.regions.SubRegion;
import orc.runtime.sites.PartialSite;
import orc.runtime.sites.Site;
import orc.runtime.values.Closure;
import orc.type.Type;
import orc.type.structured.ArrowType;

/**
 * Implements the Site site.
 * 
 * This site promotes an Orc closure to a site, making it strict in its arguments,
 * requiring that it return only one value (subsequent returns are ignored), and
 * protecting it from termination.
 * 
 * @author dkitchin
 */
public class SiteSite extends PartialSite {
	@Override
	public Type type() {
		return new SitemakerType();
	}

	@Override
	public Object evaluate(final Args args) throws TokenException {
		try {
			final Closure f = (Closure) args.getArg(0);
			return new ClosureBackedSite(f);
		} catch (final ClassCastException e) {
			// Argument must be a closure
			return null;
		}
	}

}

class SitemakerType extends Type {

	/**
	 * Ensure that there is exactly one argument, and it is an
	 * ArrowType, since all closures are of ArrowType.
	 * 
	 * The typechecker may still admit cases where sites which
	 * present type ArrowType are allowed to be arguments to
	 * this site. Such errors will be caught at runtime.
	 */
	@Override
	public Type call(final List<Type> args) throws TypeException {
		if (args.size() == 1) {
			final Type T = args.get(0);
			if (T instanceof ArrowType) {
				return T;
			} else {
				throw new TypeException("Expected a closure; got a value of type " + T + " instead");
			}
		} else {
			throw new ArgumentArityException(1, args.size());
		}
	}

}

class ClosureBackedSite extends Site {

	Closure closure;

	public ClosureBackedSite(final Closure closure) {
		this.closure = closure;
	}

	/* (non-Javadoc)
	 * @see orc.runtime.sites.Site#callSite(orc.runtime.Args, orc.runtime.Token)
	 */
	@Override
	public void callSite(final Args args, final Token caller) throws TokenException {

		final Token token = caller.getEngine().newExecution(new Stop(), // initial expr is unused 
				caller);

		token.setRegion(new ClosureExecRegion(caller, token.getRegion()));

		if (token != null) {
			final List<Object> argsList = Arrays.asList(args.asArray());
			final TokenContinuation K = new TokenContinuation() {
				public void execute(final Token t) {
					final ClosureExecRegion cer = (ClosureExecRegion) t.getRegion();
					cer.onPublish(t.getResult());
					t.die();
				}
			};
			closure.createCall(token, argsList, K);
		} else {
			throw new SiteException("Failed to host closure execution as a site.");
		}

	}

}

class ClosureExecRegion extends SubRegion {

	Token caller = null;

	public ClosureExecRegion(final Token caller, final Region parent) {
		super(parent);
		this.caller = caller;
	}

	@Override
	protected void onClose() {
		super.onClose();
		if (caller != null) {
			caller.die();
			caller = null;
		}
	}

	public void onPublish(final Object v) {
		if (caller != null) {
			caller.resume(v);
			caller = null;
		}
	}
}
