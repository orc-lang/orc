//
// SetDiscorporate.java -- Truffle node SetDiscorporate
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce;

import orc.run.porce.runtime.Counter;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;

@NodeChild(value = "c", type = Expression.class)
public abstract class SetDiscorporate extends Expression {
    @Specialization
    public PorcEUnit run(final Counter c) {
        c.setDiscorporate();
        return PorcEUnit.SINGLETON;
    }

    public static SetDiscorporate create(final Expression c) {
        return SetDiscorporateNodeGen.create(c);
    }
}
