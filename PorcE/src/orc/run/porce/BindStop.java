//
// BindStop.java -- Truffle node BindStop
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
public abstract class BindStop extends Expression {
    @Specialization
    public PorcEUnit bindFutureStop(final Future future) {
        future.stop();
        return PorcEUnit.SINGLETON;
    }

    public static BindStop create(final Expression future) {
        return BindStopNodeGen.create(future);
    }
}
