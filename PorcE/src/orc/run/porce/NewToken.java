//
// NewToken.java -- Truffle node NewToken
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
import orc.run.porce.runtime.PorcEExecution;

import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;

@NodeChild("counter")
@ImportStatic(SpecializationConfiguration.class)
public class NewToken extends Expression {
    private final BranchProfile resurrectProfile = BranchProfile.create();
    private final Counter.NewTokenContext ctx;

    protected final PorcEExecution execution;

    protected NewToken(final PorcEExecution execution) {
        this.execution = execution;
        this.ctx = new Counter.NewTokenContextImpl(execution.runtime());
    }

    @Specialization(guards = { "SpecializeOnCounterStates" })
    public PorcEUnit specialized(final Counter counter) {
        if (counter.newTokenOptimized(ctx)) {
            resurrectProfile.enter();
            counter.doResurrect();
        }
        return PorcEUnit.SINGLETON;
    }

    @Specialization
    public PorcEUnit run(final Counter counter) {
        counter.newToken();
        return PorcEUnit.SINGLETON;
    }

    public static NewToken create(final Expression parent, final PorcEExecution execution) {
        return NewTokenNodeGen.create(execution, parent);
    }
}
