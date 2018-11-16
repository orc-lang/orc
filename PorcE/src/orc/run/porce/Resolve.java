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

import static orc.run.porce.SpecializationConfiguration.InlineForceHalted;
import static orc.run.porce.SpecializationConfiguration.InlineForceResolved;

import orc.FutureState;
import orc.run.porce.call.Dispatch;
import orc.run.porce.call.InternalCPSDispatch;
import orc.run.porce.profiles.ResettableBranchProfile;
import orc.run.porce.runtime.Counter;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecution;
import orc.run.porce.runtime.Resolver;
import orc.run.porce.runtime.Terminator;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;

public abstract class Resolve {
    public static boolean isNonFuture(final Object v) {
        return !(v instanceof orc.Future);
    }

    protected static final class HandleFuture extends Node {
        final ResettableBranchProfile boundFuture = ResettableBranchProfile.create();
        final ResettableBranchProfile unboundFuture = ResettableBranchProfile.create();
        private final ConditionProfile orcFuture = ConditionProfile.createBinaryProfile();
        private final ConditionProfile porcEFuture = ConditionProfile.createBinaryProfile();
        private final ConditionProfile nonFuture = ConditionProfile.createBinaryProfile();

        public Object handleFuture(PorcENode self, final Object future) {
            CompilerAsserts.compilationConstant(self);
            CompilerAsserts.compilationConstant(this);
            CompilerAsserts.compilationConstant(nonFuture);

            if (InlineForceResolved && nonFuture.profile(isNonFuture(future))) {
                return null;
            } else if (porcEFuture.profile(future instanceof orc.run.porce.runtime.Future)) {
                final Object state = ((orc.run.porce.runtime.Future) future).getInternal();
                if (InlineForceResolved && state != orc.run.porce.runtime.FutureConstants.Unbound) {
                    if (boundFuture.enter()) {
                        unboundFuture.reset();
                    }
                    // No need to return actual value.
                    return orc.run.porce.runtime.FutureConstants.Halt;
                } else {
                    unboundFuture.enter();
                    return orc.run.porce.runtime.FutureConstants.Unbound;
                }
            } else if (orcFuture.profile(future instanceof orc.Future)) {
                final FutureState state = ((orc.Future) future).get();
                if (InlineForceResolved && (state instanceof orc.FutureState.Bound
                        || state == orc.run.porce.runtime.FutureConstants.Orc_Stopped)) {
                    if (boundFuture.enter()) {
                        unboundFuture.reset();
                    }
                    // No need to return actual value.
                    return orc.run.porce.runtime.FutureConstants.Halt;
                } else {
                    unboundFuture.enter();
                    return orc.run.porce.runtime.FutureConstants.Unbound;
                }
            } else if (!InlineForceResolved || !InlineForceHalted) {
                if (isNonFuture(future)) {
                    return future;
                } else {
                    return orc.run.porce.runtime.FutureConstants.Unbound;
                }
            } else {
                throw InternalPorcEError.unreachable(self);
            }
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("HandleFuture(");
            sb.append("nonFuture=");
            sb.append(nonFuture);
            sb.append(",porcEFuture=");
            sb.append(porcEFuture);
            sb.append(",orcFuture=");
            sb.append(orcFuture);
            sb.append(",boundFuture=");
            sb.append(boundFuture);
            sb.append(",unboundFuture=");
            sb.append(unboundFuture);
            sb.append(")");
            return sb.toString();
        }

        public static HandleFuture create() {
            return new HandleFuture();
        }
    }

    @NodeChild(value = "p", type = Expression.class)
    @NodeChild(value = "c", type = Expression.class)
    @NodeChild(value = "t", type = Expression.class)
    @NodeField(name = "nFutures", type = int.class)
    @NodeField(name = "execution", type = PorcEExecution.class)
    public static abstract class New extends Expression {
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
    public static abstract class Future extends Expression {
        @Specialization(guards = { "isNonFuture(future)" })
        public PorcEUnit nonFuture(final int index, final Resolver join, final Object future) {
            join.set(index);
            return PorcEUnit.SINGLETON;
        }

        @Specialization
        public PorcEUnit porceFuture(final int index, final Resolver join, final orc.run.porce.runtime.Future future,
                @Cached("create()") HandleFuture handleFuture) {
            Object v = handleFuture.handleFuture(this, future);

            if (v == orc.run.porce.runtime.FutureConstants.Unbound) {
                handleFuture.unboundFuture.enter();
                join.force(index, future);
            } else {
                // Handle bound and halted the same.
                handleFuture.boundFuture.enter();
                join.set(index);
            }
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

        @Override
        public void setTail(boolean b) {
            super.setTail(b);
            if (call != null) {
                call.setTail(b);
            }
        }

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
                    Dispatch n = insert(InternalCPSDispatch.create(execution, isTail));
                    n.setTail(isTail);
                    n.forceInline();
                    notifyInserted(n);
                    return n;
                });
            }
            call.dispatch(frame, join.p());
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
