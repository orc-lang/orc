//
// UMinus.java -- Java class UMinus
// Project OrcScala
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
import orc.values.sites.TypedSite;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.Args.NumericUnaryOperator;
import orc.values.sites.compatibility.EvalSite;
import orc.values.sites.compatibility.Types;

/**
 * @author dkitchin
 */
public class UMinus extends EvalSite implements TypedSite {
    @SuppressWarnings("synthetic-access")
    private static final MyOperator op = new MyOperator();

    private static final class MyOperator implements NumericUnaryOperator<Number> {
        @Override
        public Number apply(final BigInteger a) {
            return a.negate();
        }

        @Override
        public Number apply(final BigDecimal a) {
            return a.negate();
        }

        @Override
        public Number apply(final int a) {
            return Integer.valueOf(-a);
        }

        @Override
        public Number apply(final long a) {
            return Long.valueOf(-a);
        }

        @Override
        public Number apply(final byte a) {
            return Integer.valueOf(-a);
        }

        @Override
        public Number apply(final short a) {
            return Integer.valueOf(-a);
        }

        @Override
        public Number apply(final double a) {
            return Double.valueOf(-a);
        }

        @Override
        public Number apply(final float a) {
            return Float.valueOf(-a);
        }
    }

    @Override
    public Object evaluate(final Args args) throws TokenException {
        return Args.applyNumericOperator(args.numberArg(0), op);
    }

    @Override
    public Type orcType() {
        return Types.overload(Types.function(Types.integer(), Types.integer()), Types.function(Types.number(), Types.number()));
    }

    @Override
    public boolean nonBlocking() {
        return true;
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
