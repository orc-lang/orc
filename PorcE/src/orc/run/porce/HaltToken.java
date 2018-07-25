//
// HaltToken.java -- Truffle node HaltToken
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce;

import orc.run.porce.call.Dispatch;
import orc.run.porce.call.InternalCPSDispatch;
import orc.run.porce.runtime.Counter;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecution;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Introspectable;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;

@NodeChild("counter")
public class HaltToken extends Expression {

    protected final PorcEExecution execution;

    protected HaltToken(final PorcEExecution execution) {
        this.execution = execution;
    }

    @Specialization
    public PorcEUnit any(VirtualFrame frame, final Counter counter,
            @Cached("create(execution)") KnownCounter known) {
        CompilerDirectives.interpreterOnly(() -> {
            known.setTail(isTail);
        });
        known.execute(frame, counter);
        return PorcEUnit.SINGLETON;
    }

    public static HaltToken create(final Expression parent, final PorcEExecution execution) {
        return HaltTokenNodeGen.create(execution, parent);
    }

    @Introspectable
    @ImportStatic(SpecializationConfiguration.class)
    public static abstract class KnownCounter extends NodeBase {

        private final PorcEExecution execution;

        protected KnownCounter(final PorcEExecution execution) {
            this.execution = execution;
        }

        private static Object[] EMPTY_ARGS = new Object[0];

        public abstract PorcEUnit execute(VirtualFrame frame, final Counter counter);

        @Specialization(guards = { "SpecializeOnCounterStates" })
        public PorcEUnit nested(VirtualFrame frame, final Counter counter,
                @Cached("createCall()") Dispatch call,
                @Cached("create()") BranchProfile hasContinuationProfile) {
            PorcEClosure cont = counter.haltTokenOptimized();
            if (cont != null) {
                hasContinuationProfile.enter();
                Object old = SimpleWorkStealingSchedulerWrapper.currentSchedulable();
                SimpleWorkStealingSchedulerWrapper.enterSchedulable(counter, SimpleWorkStealingSchedulerWrapper.InlineExecution);
                try {
                    call.executeDispatch(frame, cont, EMPTY_ARGS);
                } finally {
                    SimpleWorkStealingSchedulerWrapper.exitSchedulable(counter, old);
                }
            }
            return PorcEUnit.SINGLETON;
        }

        @Specialization
        public PorcEUnit disabled(VirtualFrame frame, final Counter counter) {
            counter.haltToken();
            return PorcEUnit.SINGLETON;
        }

        protected Dispatch createCall() {
            Dispatch n = InternalCPSDispatch.create(false, execution, isTail);
            n.setTail(isTail);
            return n;
        }

        public static KnownCounter create(final PorcEExecution execution) {
            return HaltTokenNodeGen.KnownCounterNodeGen.create(execution);
        }
    }
}
