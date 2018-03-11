//
// Expression.java -- Truffle abstract node class Expression
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce;

import orc.util.ExpressionTag;

import com.oracle.truffle.api.instrumentation.Instrumentable;

@Instrumentable(factory = ExpressionWrapper.class)
public abstract class Expression extends PorcENode {
	@Override
	protected boolean isTaggedWith(Class<?> tag) {
		if (tag == ExpressionTag.class) {
			return true;
		} else {
			return super.isTaggedWith(tag);
		}
	}
}
