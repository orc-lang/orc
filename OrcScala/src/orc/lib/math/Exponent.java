//
// Exponent.java -- Java class Exponent
// Project OrcScala
//
// Copyright (c) 2012 The University of Texas at Austin. All rights reserved.
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
import orc.values.sites.TypedSite;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.Args.NumericBinaryOperator;
import orc.values.sites.compatibility.EvalSite;
import orc.values.sites.compatibility.Types;

public class Exponent extends EvalSite implements TypedSite {
    @SuppressWarnings("synthetic-access")
    private static final MyOperator op = new MyOperator();

    private static final class MyOperator implements NumericBinaryOperator<Number> {
        @Override
        public Number apply(final BigInteger a, final BigInteger b) {
            if (a.signum() == 0) {
                return b.signum() == 0 ? BigInteger.ONE : a;
            }
            if (a.equals(BigInteger.ONE)) {
                return a;
            }
            if (b.bitLength() >= Integer.SIZE) {
                throw new ArithmeticException("Exponent out of range");
            }
            return a.pow(b.intValue());
        }

        @Override
        public Number apply(final BigDecimal a, final BigDecimal b) {
            try {
                // Arbitrary-precision exponentiation only works if the exponent
                // is integral
                return a.pow(b.intValueExact());
            } catch (final ArithmeticException e) {
                // If the exponent is fractional or out of range, just use
                // native double exponentiation
                // This _can_ lose precision.
                return Double.valueOf(java.lang.Math.pow(a.doubleValue(), b.doubleValue()));
            }
        }

        @Override
        public Number apply(final int a, final int b) {
            return Double.valueOf(java.lang.Math.pow(a, b));
        }

        @Override
        public Number apply(final long a, final long b) {
            return Double.valueOf(java.lang.Math.pow(a, b));
        }

        @Override
        public Number apply(final byte a, final byte b) {
            return Double.valueOf(java.lang.Math.pow(a, b));
        }

        @Override
        public Number apply(final short a, final short b) {
            return Double.valueOf(java.lang.Math.pow(a, b));
        }

        @Override
        public Number apply(final double a, final double b) {
            return Double.valueOf(java.lang.Math.pow(a, b));
        }

        @Override
        public Number apply(final float a, final float b) {
            return Double.valueOf(java.lang.Math.pow(a, b));
        }
    }

    @Override
    public Object evaluate(final Args args) throws TokenException {
        return Args.applyNumericOperator(args.numberArg(0), args.numberArg(1), op);
    }

    @Override
    public Type orcType() {
        return Types.function(Types.number(), Types.number(), Types.number());
    }

    @Override
    public boolean nonBlocking() {
        return true;
    }

    @Override
    public int minPublications() {
        return 0;
    }

    @Override
    public int maxPublications() {
        return 1;
    }

    @Override
    public boolean effectFree() {
        return true;
    }
}
