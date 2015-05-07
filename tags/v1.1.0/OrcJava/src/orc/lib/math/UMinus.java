//
// UMinus.java -- Java class UMinus
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
import orc.runtime.Args.NumericUnaryOperator;
import orc.runtime.sites.EvalSite;
import orc.type.Type;
import orc.type.structured.ArrowType;
import orc.type.structured.MultiType;

/**
 * @author dkitchin
 *
 */
public class UMinus extends EvalSite {
	private static final MyOperator op = new MyOperator();

	private static final class MyOperator implements NumericUnaryOperator<Number> {
		public Number apply(final BigInteger a) {
			return a.negate();
		}

		public Number apply(final BigDecimal a) {
			return a.negate();
		}

		public Number apply(final int a) {
			return -a;
		}

		public Number apply(final long a) {
			return -a;
		}

		public Number apply(final byte a) {
			return -a;
		}

		public Number apply(final short a) {
			return -a;
		}

		public Number apply(final double a) {
			return -a;
		}

		public Number apply(final float a) {
			return -a;
		}
	}

	@Override
	public Object evaluate(final Args args) throws TokenException {
		return Args.applyNumericOperator(args.numberArg(0), op);
	}

	@Override
	public Type type() {
		return new MultiType(new ArrowType(Type.INTEGER, Type.INTEGER), new ArrowType(Type.NUMBER, Type.NUMBER));
	}
}
