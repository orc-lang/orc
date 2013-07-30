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
import orc.types.Type;
import orc.values.sites.DirectSite;
import orc.values.sites.TypedSite;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.Args.NumericBinaryOperator;
import orc.values.sites.compatibility.EvalSite;
import orc.values.sites.compatibility.Types;

@SuppressWarnings("synthetic-access")
public class Div extends EvalSite implements TypedSite, DirectSite {
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
				return Double.valueOf(a.doubleValue() / b.doubleValue());
			}
		}

		@Override
		public Number apply(final int a, final int b) {
			return Integer.valueOf(a / b);
		}

		@Override
		public Number apply(final long a, final long b) {
			return Long.valueOf(a / b);
		}

		@Override
		public Number apply(final byte a, final byte b) {
			return Integer.valueOf(a / b);
		}

		@Override
		public Number apply(final short a, final short b) {
			return Integer.valueOf(a / b);
		}

		@Override
		public Number apply(final double a, final double b) {
			return Double.valueOf(a / b);
		}

		@Override
		public Number apply(final float a, final float b) {
			return Float.valueOf(a / b);
		}
	}

	@Override
	public Object evaluate(final Args args) throws TokenException {
		return Args.applyNumericOperator(args.numberArg(0), args.numberArg(1), op);
	}

	@Override
	public Type orcType() {
		return Types.overload(Types.function(Types.integer(), Types.integer(), Types.integer()), Types.function(Types.number(), Types.number(), Types.number()));
	}

	@Override
    public boolean effectFree() { return true; }
}
