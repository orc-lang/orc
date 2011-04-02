//
// Div.java -- Java class Div
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
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.EvalSite;
import orc.values.sites.compatibility.Args.NumericBinaryOperator;
import orc.values.sites.compatibility.type.Type;
import orc.values.sites.compatibility.type.structured.ArrowType;
import orc.values.sites.compatibility.type.structured.MultiType;

@SuppressWarnings({ "boxing", "synthetic-access" })
public class Div extends EvalSite {
	private static final MyOperator op = new MyOperator();

	private static final class MyOperator implements NumericBinaryOperator<Number> {
		@Override
		public Number apply(final BigInteger a, final BigInteger b) {
			return a.divide(b);
		}

		@Override
		public Number apply(final BigDecimal a, final BigDecimal b) {
			try {
				return a.divide(b);
			} catch (final ArithmeticException _) {
				// an exception is thrown if the dividend is
				// not representable as a finite decimal, so
				// in that case we convert to double.
				// warning: this can lose precision
				return a.doubleValue() / b.doubleValue();
			}
		}

		@Override
		public Number apply(final int a, final int b) {
			return a / b;
		}

		@Override
		public Number apply(final long a, final long b) {
			return a / b;
		}

		@Override
		public Number apply(final byte a, final byte b) {
			return a / b;
		}

		@Override
		public Number apply(final short a, final short b) {
			return a / b;
		}

		@Override
		public Number apply(final double a, final double b) {
			return a / b;
		}

		@Override
		public Number apply(final float a, final float b) {
			return a / b;
		}
	}

	@Override
	public Object evaluate(final Args args) throws TokenException {
		return Args.applyNumericOperator(args.numberArg(0), args.numberArg(1), op);
	}

	@Override
	public Type type() {
		return new MultiType(new ArrowType(Type.INTEGER, Type.INTEGER, Type.INTEGER), new ArrowType(Type.NUMBER, Type.NUMBER, Type.NUMBER));
	}
}