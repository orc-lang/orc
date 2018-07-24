//
// Resolve.java -- Truffle node Resolve
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
import orc.run.porce.runtime.Resolver;
import orc.run.porce.runtime.Terminator;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

public class Resolve extends Expression {
    public static boolean isNonFuture(final Object v) {
        return !(v instanceof orc.Future);
    }

    @NodeChild(value = "p", type = Expression.class)
    @NodeChild(value = "c", type = Expression.class)
    @NodeChild(value = "t", type = Expression.class)
    @NodeField(name = "nFutures", type = int.class)
    @NodeField(name = "execution", type = PorcEExecution.class)
    public static class New extends Expression {
        @Specialization
        public Object run(final int nFutures, final PorcEExecution execution, final PorcEClosure p, final Counter c, final Terminator t) {
            return new Resolver(p, c, t, nFutures, execution);
        }

        public static New create(final Expression p, final Expression c, final Expression t, final int nFutures, final PorcEExecution execution) {
            return ResolveFactory.NewNodeGen.create(p, c, t, nFutures, execution);
        }
    }

    @NodeChild(value = "join", type = Expression.class)
    @NodeChild(value = "future", type = Expression.class)
    @NodeField(name = "index", type = int.class)
    @ImportStatic({ Force.class })
    public static class Future extends Expression {
        @Specialization(guards = { "isNonFuture(future)" })
        public PorcEUnit nonFuture(final int index, final Resolver join, final Object future) {
            join.set(index);
            return PorcEUnit.SINGLETON;
        }

        // TODO: PERFORMANCE: It may be worth playing with specializing by
        // future states. Futures that are always bound may be common.
        // This would only be worth it if resolve is called frequently.
        @Specialization
        public PorcEUnit porceFuture(final int index, final Resolver join, final orc.run.porce.runtime.Future future) {
            join.force(index, future);
            return PorcEUnit.SINGLETON;
        }

        @Specialization(replaces = { "porceFuture" })
        public PorcEUnit unknown(final int index, final Resolver join, final orc.Future future) {
            join.force(index, future);
            return PorcEUnit.SINGLETON;
        }

        public static Future create(final Expression join, final Expression future, final int index) {
            return ResolveFactory.FutureNodeGen.create(join, future, index);
        }
    }

    @NodeChild(value = "join", type = Expression.class)
    @NodeField(name = "execution", type = PorcEExecution.class)
    @ImportStatic(SpecializationConfiguration.class)
    public static abstract class Finish extends Expression {
        protected static final boolean TRUE = true;

        @Child
        protected Dispatch call = null;

        @Specialization(guards = { "join.isBlocked()", "TRUE" })
        public PorcEUnit blocked(final VirtualFrame frame,
                final Resolver join,
                @Cached("create(1, execution)") FlushAllCounters flushAllCounters) {
            // Flush positive counters because this may trigger our continuation to execute in another thread.
            flushAllCounters.execute(frame);
            join.finishBlocked();
            return PorcEUnit.SINGLETON;
        }

        @Specialization(guards = { "InlineForceResolved", "join.isResolved()" }, replaces = { "blocked" })
        public PorcEUnit resolved(final VirtualFrame frame, final PorcEExecution execution, final Resolver join) {
                if (call == null) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        computeAtomicallyIfNull(() -> call, (v) -> call = v, () -> {
                                Dispatch n = insert(InternalCPSDispatch.create(false, execution, isTail));
                                n.setTail(isTail);
                                notifyInserted(n);
                                return n;
                        });
                }
            call.executeDispatch(frame, join.p(), new Object[] {});
            return PorcEUnit.SINGLETON;
        }

        @Specialization(guards = { "join.isBlocked()" })
        public PorcEUnit blockedAgain(final VirtualFrame frame,
                final Resolver join,
                @Cached("create(1, execution)") FlushAllCounters flushAllCounters) {
            return blocked(frame, join, flushAllCounters);
        }

        @Specialization(guards = { "!InlineForceResolved" })
        public PorcEUnit fallback(final PorcEExecution execution, final Resolver join) {
            join.finish();
            return PorcEUnit.SINGLETON;
        }

        public static Finish create(final Expression join, final PorcEExecution execution) {
            return ResolveFactory.FinishNodeGen.create(join, execution);
        }
    }
}
