//
// ComparisonSite.java -- Java class ComparisonSite
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

package orc.lib.comp;

import java.math.BigDecimal;
import java.math.BigInteger;

import orc.error.runtime.ArgumentTypeMismatchException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Args.NumericBinaryOperator;
import orc.runtime.sites.EvalSite;
import orc.type.Type;
import orc.type.structured.ArrowType;

/**
 * @author quark
 */
public abstract class ComparisonSite extends EvalSite {
	private static class MyOperator implements NumericBinaryOperator<Integer> {
		public Integer apply(final BigInteger a, final BigInteger b) {
			return a.compareTo(b);
		}

		public Integer apply(final BigDecimal a, final BigDecimal b) {
			return a.compareTo(b);
		}

		public Integer apply(final int a, final int b) {
			return a - b;
		}

		public Integer apply(final long a, final long b) {
			return (int) (a - b);
		}

		public Integer apply(final byte a, final byte b) {
			return a - b;
		}

		public Integer apply(final short a, final short b) {
			return a - b;
		}

		public Integer apply(final double a, final double b) {
			return Double.compare(a, b);
		}

		public Integer apply(final float a, final float b) {
			return Float.compare(a, b);
		}
	}

	/* (non-Javadoc)
	 * @see orc.runtime.sites.EvalSite#evaluate(java.lang.Object[])
	 */
	@Override
	public Object evaluate(final Args args) throws TokenException {
		final Object arg0 = args.getArg(0);
		final Object arg1 = args.getArg(1);
		try {
			if (arg0 instanceof Number && arg1 instanceof Number) {
				final int a = Args.applyNumericOperator((Number) arg0, (Number) arg1, new MyOperator());
				return compare(a);
			} else {
				final int a = ((Comparable) arg0).compareTo(arg1);
				return compare(a);
			}
		} catch (final ClassCastException e) {
			throw new ArgumentTypeMismatchException(e);
		}
	}

	abstract public boolean compare(int a);

	@Override
	public Type type() {
		return new ArrowType(Type.TOP, Type.TOP, Type.BOOLEAN);
	}
}
