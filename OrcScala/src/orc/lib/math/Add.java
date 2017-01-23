//
// Add.java -- Java class Add
// Project OrcScala
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.math;

import java.math.BigDecimal;
import java.math.BigInteger;

import orc.error.runtime.ArgumentTypeMismatchException;
import orc.error.runtime.TokenException;
import orc.types.RecordExtensorType;
import orc.types.Type;
import orc.values.sites.TypedSite;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.Args.NumericBinaryOperator;
import orc.values.sites.compatibility.EvalSite;
import orc.values.sites.compatibility.Types;

/**
 * NB: this is overloaded to operate on strings, with implicit toString coercion
 * (just like Java).
 * 
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
        Object a = args.getArg(0);
        Object b = args.getArg(1);

        if (a instanceof Number && b instanceof Number) {
            return Args.applyNumericOperator(args.numberArg(0), args.numberArg(1), op);
        } else if (a instanceof String) {
            return (String) a + orc.values.Format.formatValueR(args.getArg(1), false);
        } else if (b instanceof String) {
            return orc.values.Format.formatValueR(args.getArg(0), false) + (String) b;
        } else if (a instanceof orc.values.OrcRecord || b instanceof orc.values.OrcRecord) {
            return ((orc.values.OrcRecord) a).extendWith((orc.values.OrcRecord) b);
        } else {
            // TODO: This is not accurate, since we don't know WHICH parameter has the wrong time. However the old version wasn't very specific either.
            throw new ArgumentTypeMismatchException(0, "Number, String, or record", a != null ? a.getClass().toString() : "null");
        }
    }

    @Override
    public Type orcType() {
        return Types.overload(Types.function(Types.integer(), Types.integer(), Types.integer()), Types.function(Types.number(), Types.number(), Types.number()), new RecordExtensorType(), Types.function(Types.string(), Types.top(), Types.string()), Types.function(Types.top(), Types.string(), Types.string()));
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
