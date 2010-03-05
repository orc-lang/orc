//
// Site.java -- Java class Site
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.runtime.sites;

import java.util.List;

import orc.ast.oil.TokenContinuation;
import orc.error.compiletime.typing.TypeException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.values.Callable;
import orc.runtime.values.Value;
import orc.runtime.values.Visitor;
import orc.type.Type;

/**
 * Base class for all sites
 * @author wcook, dkitchin
 */
public abstract class Site extends Value implements Callable {

	/** 
	 * Invoked by a Call to invoke a site. The argument list is 
	 * scanned to make sure that all parameters are bound.
	 * If an unbound parameter is found, the call is placed on a 
	 * queue and nothing more is done.
	 * Once all parameters are bound, their values are collected
	 * and the corresponding subclass (the actual site) is called. 
	 * 
	 * @see orc.runtime.values.Callable#createCall(Token, List, TokenContinuation)
	 */
	public void createCall(final Token callToken, final List<Object> args, final TokenContinuation publishContinuation) throws TokenException {
		final Object[] values = new Object[args.size()];

		int i = 0;
		for (final Object f : args) {
			final Object v = Value.forceArg(f, callToken);
			if (v == Value.futureNotReady) {
				return;
			} else {
				values[i] = v;
			}
			++i;
		}

		callToken.getTracer().send(this, values);
		//enterTransaction(callToken.getTransaction());
		callSite(new Args(values), callToken);
	}

	/* 
	 * Attempt to add this site as a cohort to the transaction.
	 * The default behavior is to refuse to participate in any
	 * transaction by raising an exception, and to accept any
	 * call that is not within a transaction.
	 */
	/*
	protected void enterTransaction(Transaction transaction) throws NontransactionalSiteException {
		if (transaction != null) {
			throw new NontransactionalSiteException(this, transaction);
		}
	}
	*/

	/**
	 * Must be implemented by subclasses to implement the site behavior
	 * @param args			list of argument values
	 * @param caller	where the result should be sent
	 */
	abstract public void callSite(Args args, Token caller) throws TokenException;

	@Override
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}

	public Type type() throws TypeException {
		// HACK: if someone doesn't want to provide a type,
		// then we treat the site as dynamic / untyped.
		// It might be better to make this method abstract
		// to force subclasses to implement something appropriate.
		return Type.BOT;
		//throw new MissingTypeException();
	}
}
