//
// Ceil.java -- Java class Ceil
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
import orc.values.sites.compatibility.EvalSite;
import orc.values.sites.compatibility.Types;

public class Ceil extends EvalSite implements TypedSite {

    public static BigInteger ceil(final BigDecimal d) {
        if (d.signum() >= 0) {
            try {
                // d has no fractional part
                return d.toBigIntegerExact();
            } catch (final ArithmeticException e) {
                // d has a fractional part
                return d.add(BigDecimal.ONE).toBigInteger();
            }
        } else {
            return Floor.floor(d.negate()).negate();
        }
    }

    @Override
    public Object evaluate(final Args args) throws TokenException {
        final Number n = args.numberArg(0);
        if (n instanceof BigInteger || n instanceof Integer || n instanceof Long || n instanceof Short || n instanceof Byte) {
            return n;
        } else {
            final BigDecimal d = n instanceof BigDecimal ? (BigDecimal) n : new BigDecimal(n.doubleValue());
            return ceil(d);
        }
    }

    @Override
    public Type orcType() {
        return Types.function(Types.number(), Types.integer());
    }

    @Override
    public boolean nonBlocking() { return true; }
    @Override
    public int minPublications() { return 1; }
    @Override
    public int maxPublications() { return 1; }
    @Override
    public boolean effectFree() { return true; }
}
