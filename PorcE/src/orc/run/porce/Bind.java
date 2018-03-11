//
// Bind.java -- Truffle node Bind
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce;

import orc.run.porce.runtime.Future;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;

@NodeChild(value = "future", type = Expression.class)
@NodeChild(value = "value", type = Expression.class)
public abstract class Bind extends Expression {
    @Specialization
    public PorcEUnit bindFuture(final Future future, final Object value) {
        future.bind(value);
        return PorcEUnit.SINGLETON;
    }

    public static Bind create(final Expression future, final Expression value) {
        return BindNodeGen.create(future, value);
    }
}
