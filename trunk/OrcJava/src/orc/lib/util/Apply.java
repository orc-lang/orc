//
// Apply.java -- Java class Apply
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

package orc.lib.util;

import java.util.List;

import orc.ast.oil.TokenContinuation;
import orc.error.compiletime.typing.ArgumentArityException;
import orc.error.compiletime.typing.TypeException;
import orc.error.runtime.ArgumentTypeMismatchException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.Site;
import orc.runtime.values.Callable;
import orc.runtime.values.ListValue;
import orc.runtime.values.Value;
import orc.type.Type;
import orc.type.structured.ArrowType;
import orc.type.structured.EllipsisArrowType;
import orc.type.structured.ListType;

/**
 * Apply a callable to a list of arguments.
 * HACK: this is a subclass of site but has slightly different
 * semantics: the callable argument is forced as a call (i.e.
 * free variables in the callable are not forced).
 * @author quark
 */
public class Apply extends Site {
	@Override
	public void createCall(final Token caller, final List<Object> args, final TokenContinuation nextNode) throws TokenException {
		final Callable callable = Value.forceCall(args.get(0), caller);
		if (callable == Value.futureNotReady) {
			return;
		}
		final Object arguments = Value.forceArg(args.get(1), caller);
		if (arguments == Value.futureNotReady) {
			return;
		}

		if (!(arguments instanceof ListValue)) {
			throw new ArgumentTypeMismatchException(1, "ListValue", arguments.getClass().toString());
		}

		callable.createCall(caller, ((ListValue) arguments).enlist(), nextNode);
	}

	@Override
	public void callSite(final Args args, final Token caller) throws TokenException {
		// DO NOTHING
	}

	@Override
	public Type type() {

		return new Type() {

			@Override
			public Type call(final List<Type> args) throws TypeException {

				if (args.size() != 2) {
					throw new ArgumentArityException(2, args.size());
				}
				final Type funtype = args.get(0);
				final Type argtype = args.get(1).unwrapAs(new ListType());

				if (funtype instanceof ArrowType) {
					final ArrowType at = (ArrowType) funtype;
					for (final Type t : at.argTypes) {
						argtype.assertSubtype(t);
					}
					return at.resultType;
				} else if (funtype instanceof EllipsisArrowType) {
					final EllipsisArrowType et = (EllipsisArrowType) funtype;
					argtype.assertSubtype(et.repeatedArgType);
					return et.resultType;
				} else {
					throw new TypeException(funtype + " cannot be applied to an unknown number of arguments of type " + argtype);
				}
			}

		};

	}

}
