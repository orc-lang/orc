//
// Add.java -- Java class Add
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

package orc.lib.math;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Args.NumericBinaryOperator;
import orc.runtime.sites.EvalSite;
import orc.type.Type;
import orc.type.structured.ArrowType;
import orc.type.structured.MultiType;

/**
 * NB: this is overloaded to operate on strings,
 * with implicit toString coercion (just like Java).
 * @author quark
 */
public class Add extends EvalSite {
	private static final MyOperator op = new MyOperator();

	private static final class MyOperator implements NumericBinaryOperator<Number> {
		public Number apply(final BigInteger a, final BigInteger b) {
			return a.add(b);
		}

		public Number apply(final BigDecimal a, final BigDecimal b) {
			return a.add(b);
		}

		public Number apply(final int a, final int b) {
			return a + b;
		}

		public Number apply(final long a, final long b) {
			return a + b;
		}

		public Number apply(final byte a, final byte b) {
			return a + b;
		}

		public Number apply(final short a, final short b) {
			return a + b;
		}

		public Number apply(final double a, final double b) {
			return a + b;
		}

		public Number apply(final float a, final float b) {
			return a + b;
		}
	}

	@Override
	public Object evaluate(final Args args) throws TokenException {
		try {
			return Args.applyNumericOperator(args.numberArg(0), args.numberArg(1), op);
		} catch (final TokenException _1) {
			// If the arguments aren't both numbers, maybe
			// one or the other is a string
			try {
				// the first argument is a string
				final String a = args.stringArg(0);
				return a + String.valueOf(args.getArg(1));
			} catch (final TokenException _2) {
				// the second argument is a string
				final String b = args.stringArg(1);
				return String.valueOf(args.getArg(0)) + b;
			}
		}
	}

	@Override
	public Type type() {
		final List<Type> alts = new LinkedList<Type>();

		alts.add(new ArrowType(Type.INTEGER, Type.INTEGER, Type.INTEGER));
		alts.add(new ArrowType(Type.NUMBER, Type.NUMBER, Type.NUMBER));
		alts.add(new ArrowType(Type.STRING, Type.TOP, Type.STRING));
		alts.add(new ArrowType(Type.TOP, Type.STRING, Type.STRING));

		return new MultiType(alts);
	}
}
