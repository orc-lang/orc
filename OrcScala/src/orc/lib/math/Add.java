//
// Add.java -- Java class Add
// Project OrcScala
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.math;

import java.math.BigDecimal;
import java.math.BigInteger;

import orc.error.runtime.TokenException;
import orc.types.Type;
import orc.types.RecordExtensorType;
import orc.values.sites.TypedSite;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.Args.NumericBinaryOperator;
import orc.values.sites.compatibility.EvalSite;
import orc.values.sites.compatibility.Types;

/**
 * NB: this is overloaded to operate on strings,
 * with implicit toString coercion (just like Java).
 * @author quark
 */
@SuppressWarnings("synthetic-access")
public class Add extends EvalSite implements TypedSite {
	private static final MyOperator op = new MyOperator();

	private static final class MyOperator implements NumericBinaryOperator<Number> {
		@Override
		public Number apply(final BigInteger a, final BigInteger b) {
			return a.add(b);
		}

		@Override
		public Number apply(final BigDecimal a, final BigDecimal b) {
			return a.add(b);
		}

		@Override
		public Number apply(final int a, final int b) {
			return Integer.valueOf(a + b);
		}

		@Override
		public Number apply(final long a, final long b) {
			return Long.valueOf(a + b);
		}

		@Override
		public Number apply(final byte a, final byte b) {
			return Integer.valueOf(a + b);
		}

		@Override
		public Number apply(final short a, final short b) {
			return Integer.valueOf(a + b);
		}

		@Override
		public Number apply(final double a, final double b) {
			return Double.valueOf(a + b);
		}

		@Override
		public Number apply(final float a, final float b) {
			return Float.valueOf(a + b);
		}
	}

	@Override
	public Object evaluate(final Args args) throws TokenException {
		try {
			return Args.applyNumericOperator(args.numberArg(0), args.numberArg(1), op);
		} catch (final TokenException _1) {
			try {
				final orc.values.OrcRecord a = (orc.values.OrcRecord) args.getArg(0);
				final orc.values.OrcRecord b = (orc.values.OrcRecord) args.getArg(1);
				return a.extendWith(b);
			} catch (final ClassCastException _2) {
				// If the arguments aren't both numbers or records, maybe
				// one or the other is a string
				try {
					// the first argument is a string
					final String a = args.stringArg(0);
					return a + String.valueOf(args.getArg(1));
				} catch (final TokenException _3) {
					// the second argument is a string
					final String b = args.stringArg(1);
					return String.valueOf(args.getArg(0)) + b;
				}
			}
		}
	}

	@Override
	public Type orcType() {
		return Types.overload(
		    Types.function(Types.integer(), Types.integer(), Types.integer()), 
		    Types.function(Types.number(), Types.number(), Types.number()),
		    new RecordExtensorType(),
		    Types.function(Types.string(), Types.top(), Types.string()), 
		    Types.function(Types.top(), Types.string(), Types.string()));
	}
}
