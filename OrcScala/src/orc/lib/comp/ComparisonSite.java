//
// ComparisonSite.java -- Java class ComparisonSite
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

package orc.lib.comp;

import java.math.BigDecimal;
import java.math.BigInteger;

import orc.error.runtime.ArgumentTypeMismatchException;
import orc.error.runtime.TokenException;
import orc.types.Type;
import orc.values.sites.TypedSite;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.Args.NumericBinaryOperator;
import orc.values.sites.compatibility.EvalSite;
import orc.values.sites.compatibility.Types;

/**
 * @author quark
 */
@SuppressWarnings("synthetic-access")
public abstract class ComparisonSite extends EvalSite implements TypedSite {
	private static class MyOperator implements NumericBinaryOperator<Integer> {
		@Override
		public Integer apply(final BigInteger a, final BigInteger b) {
			return Integer.valueOf(a.compareTo(b));
		}

		@Override
		public Integer apply(final BigDecimal a, final BigDecimal b) {
			return Integer.valueOf(a.compareTo(b));
		}

		@Override
		public Integer apply(final int a, final int b) {
			return Integer.valueOf(a - b);
		}

		@Override
		public Integer apply(final long a, final long b) {
			return Integer.valueOf((int) (a - b));
		}

		@Override
		public Integer apply(final byte a, final byte b) {
			return Integer.valueOf(a - b);
		}

		@Override
		public Integer apply(final short a, final short b) {
			return Integer.valueOf(a - b);
		}

		@Override
		public Integer apply(final double a, final double b) {
			return Integer.valueOf(Double.compare(a, b));
		}

		@Override
		public Integer apply(final float a, final float b) {
			return Integer.valueOf(Float.compare(a, b));
		}
	}

	@Override
	public Object evaluate(final Args args) throws TokenException {
		final Object arg0 = args.getArg(0);
		final Object arg1 = args.getArg(1);
		if (arg0 instanceof Number && arg1 instanceof Number) {
			final int a = Args.applyNumericOperator((Number) arg0, (Number) arg1, new MyOperator()).intValue();
			return Boolean.valueOf(compare(a));
		} else {
			if (!(arg0 instanceof Comparable)) {
				throw new ArgumentTypeMismatchException(0, "Comparable<Object>", args.getArg(0).getClass().getCanonicalName());
			}
			@SuppressWarnings("unchecked")
			final int a = ((Comparable<Object>) arg0).compareTo(arg1);
			return Boolean.valueOf(compare(a));
		}
	}

	abstract public boolean compare(int a);

	@Override
	public Type orcType() {
		return Types.function(Types.top(), Types.top(), Types.bool());
	}
}
