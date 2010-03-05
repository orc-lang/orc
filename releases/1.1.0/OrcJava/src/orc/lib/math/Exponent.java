//
// Exponent.java -- Java class Exponent
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

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Args.NumericBinaryOperator;
import orc.runtime.sites.EvalSite;
import orc.type.Type;
import orc.type.structured.ArrowType;

public class Exponent extends EvalSite {
	private static final MyOperator op = new MyOperator();

	private static final class MyOperator implements NumericBinaryOperator<Number> {
		public Number apply(final BigInteger a, final BigInteger b) {
			return a.pow(b.intValue());
		}

		public Number apply(final BigDecimal a, final BigDecimal b) {

			try {
				// Arbitrary-precision exponentiation only works if the exponent is integral
				return a.pow(b.intValueExact());
			} catch (final ArithmeticException e) {
				// If the exponent is fractional or out of range, just use native double exponentiation
				// This _can_ lose precision.
				return java.lang.Math.pow(a.doubleValue(), b.doubleValue());
			}

		}

		public Number apply(final int a, final int b) {
			return java.lang.Math.pow(a, b);
		}

		public Number apply(final long a, final long b) {
			return java.lang.Math.pow(a, b);
		}

		public Number apply(final byte a, final byte b) {
			return java.lang.Math.pow(a, b);
		}

		public Number apply(final short a, final short b) {
			return java.lang.Math.pow(a, b);
		}

		public Number apply(final double a, final double b) {
			return java.lang.Math.pow(a, b);
		}

		public Number apply(final float a, final float b) {
			return java.lang.Math.pow(a, b);
		}
	}

	@Override
	public Object evaluate(final Args args) throws TokenException {
		return Args.applyNumericOperator(args.numberArg(0), args.numberArg(1), op);
	}

	@Override
	public Type type() {
		return new ArrowType(Type.NUMBER, Type.NUMBER, Type.NUMBER);
	}
}
