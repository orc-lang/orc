//
// Closure.java -- Java class Closure
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

package orc.runtime.values;

import java.util.Iterator;
import java.util.List;

import orc.ast.oil.TokenContinuation;
import orc.ast.oil.expression.Def;
import orc.env.Env;
import orc.error.runtime.ArityMismatchException;
import orc.error.runtime.TokenException;
import orc.error.runtime.UncallableValueException;
import orc.runtime.Token;

/**
 * Represents a standard closure: a function defined in an environment.
 * 
 * <p>A closure is not necessarily a resolved value, since it may contain
 * unbound variables, and therefore cannot be used in arg position until all
 * such variables become bound.
 * 
 * @author wcook, dkitchin, quark
 */
public final class Closure extends Value implements Callable, Future {
	public Def def;
	public Env env = null;
	private List<Object> free = null;

	/**
	 * The environment should be set later; see {@link orc.ast.extended.declaration.DefsDeclaration}.
	 */
	public Closure(final Def def, final List<Object> free) {
		this.def = def;
		this.free = free;
	}

	public void createCall(final Token t, final List<Object> args, final TokenContinuation publishContinuation) throws TokenException {
		if (args.size() != def.arity) {
			throw new ArityMismatchException(def.arity, args.size());
		}

		t.enterClosure(this, publishContinuation);
		for (final Object f : args) {
			t.bind(f);
		}
		t.activate();
	}

	@Override
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}

	public Object forceArg(final Token t) {
		if (free != null) {
			final Iterator<Object> freei = free.iterator();
			while (freei.hasNext()) {
				if (Value.forceArg(freei.next(), t) == Value.futureNotReady) {
					return Value.futureNotReady;
				}
				freei.remove();
			}
			free = null;
		}
		return this;
	}

	public Callable forceCall(final Token t) throws UncallableValueException {
		return this;
	}
}
