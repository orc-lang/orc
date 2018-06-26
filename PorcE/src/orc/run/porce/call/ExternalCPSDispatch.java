//
// ExternalCPSDispatch.java -- Java class ExternalCPSDispatch
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.call;

import static orc.run.porce.SpecializationConfiguration.ExternalCPSDirectSpecialization;

import orc.DirectInvoker;
import orc.Invoker;
import orc.error.runtime.HaltException;
import orc.run.porce.StackCheckingDispatch;
import orc.run.porce.ValueClassesProfile;
import orc.run.porce.SpecializationConfiguration;
import orc.run.porce.runtime.CPSCallContext;
import orc.run.porce.runtime.Counter;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecution;
import orc.run.porce.runtime.PorcERuntime;
import orc.run.porce.runtime.TailCallException;
import orc.run.porce.runtime.Terminator;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Introspectable;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Instrumentable;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;

public class ExternalCPSDispatch extends Dispatch {
    @Child
    protected ExternalCPSDispatch.ExternalCPSDispatchInternal internal;

    protected ExternalCPSDispatch(final PorcEExecution execution) {
        super(execution);
        internal = ExternalCPSDispatch.ExternalCPSDispatchInternal.createBare(execution);
    }

    protected ExternalCPSDispatch(final ExternalCPSDispatch orig) {
        super(orig.internal.execution);
        internal = ExternalCPSDispatch.ExternalCPSDispatchInternal.createBare(orig.internal.execution);
    }

    @Override
    public void setTail(boolean v) {
        super.setTail(v);
        internal.setTail(v);
    }

    @Override
    public void executeDispatch(VirtualFrame frame, Object target, Object[] arguments) {
        PorcEClosure pub = (PorcEClosure) arguments[0];
        Counter counter = (Counter) arguments[1];
        Terminator term = (Terminator) arguments[2];
        internal.execute(frame, target, pub, counter, term, buildArguments(0, arguments));
    }

    @Override
    public void executeDispatchWithEnvironment(VirtualFrame frame, Object target, Object[] arguments) {
        PorcEClosure pub = (PorcEClosure) arguments[1];
        Counter counter = (Counter) arguments[2];
        Terminator term = (Terminator) arguments[3];
        internal.execute(frame, target, pub, counter, term, buildArguments(1, arguments));
    }

    @SuppressWarnings({ "boxing" })
    protected static Object[] buildArguments(int offset, Object[] arguments) {
        CompilerAsserts.compilationConstant(arguments.length);
        Object[] newArguments = new Object[arguments.length - 3 - offset];
        System.arraycopy(arguments, 3 + offset, newArguments, 0, newArguments.length);
        return newArguments;
    }

    static ExternalCPSDispatch createBare(PorcEExecution execution) {
        return new ExternalCPSDispatch(execution);
    }

    @ImportStatic({ SpecializationConfiguration.class })
    @Introspectable
    @Instrumentable(factory = ExternalCPSDispatchInternalWrapper.class)
    public static abstract class ExternalCPSDispatchInternal extends DispatchBase {
        protected ExternalCPSDispatchInternal(final PorcEExecution execution) {
            super(execution);
        }

        protected ExternalCPSDispatchInternal(final ExternalCPSDispatchInternal orig) {
            super(orig.execution);
        }

        protected final BranchProfile exceptionProfile = BranchProfile.create();
        protected final BranchProfile haltProfile = BranchProfile.create();

        protected final ValueClassesProfile argumentClassesProfile = new ValueClassesProfile();

        @CompilerDirectives.CompilationFinal
        protected StackCheckingDispatch dispatchP = null;

        protected StackCheckingDispatch getDispatchP() {
            if (dispatchP == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                computeAtomicallyIfNull(() -> dispatchP, (v) -> dispatchP = v, () -> {
                    StackCheckingDispatch n = insert(StackCheckingDispatch.create(execution));
                    n.setTail(isTail);
                    notifyInserted(n);
                    return n;
                });
            }
            return dispatchP;
        }

        public abstract void execute(VirtualFrame frame, Object target, PorcEClosure pub, Counter counter,
                Terminator term, Object[] arguments);

        @Specialization(guards = {
                "ExternalCPSDirectSpecialization", "invoker != null",
                "canInvoke.executeCanInvoke(frame, invoker, target, argumentClassesProfile.profile(arguments))" },
            limit = "ExternalDirectCallMaxCacheSize")
        public void specificDirect(final VirtualFrame frame, final Object target, PorcEClosure pub, Counter counter,
                Terminator term, final Object[] arguments,
                @Cached("getDirectInvokerWithBoundary(target, arguments)") DirectInvoker invoker,
                @Cached("create()") InvokerCanInvoke canInvoke,
                @Cached("create()") InvokerInvokeDirect invokeDirect) {
            // DUPLICATION: This code is duplicated (mostly) in ExternalDirectDispatch.specific.
            try {
                final Object v;
                try {
                    v = invokeDirect.executeInvokeDirect(frame, invoker, target, argumentClassesProfile.profile(arguments));
                } finally {
                    if (SpecializationConfiguration.StopWatches.callsEnabled) {
                        orc.run.StopWatches.callTime()
                                .stop(FrameUtil.getLongSafe(frame, Call.getCallStartTimeSlot(this)));
                    }
                }
                getDispatchP().executeDispatch(frame, pub, v);
            } catch (final TailCallException e) {
                throw e;
            } catch (final HaltException e) {
                haltProfile.enter();
                counter.haltToken();
            } catch (final Throwable e) {
                exceptionProfile.enter();
                execution.notifyOfException(e, this);
                counter.haltToken();
            }
            // Token: All exception handlers halt the token that was passed to this
            // call. Calls are not allowed to keep the token if they throw an
            // exception.
        }

        @Specialization(guards = {
                "isNotDirectInvoker(invoker) || !ExternalCPSDirectSpecialization",
                "canInvoke.executeCanInvoke(frame, invoker, target, argumentClassesProfile.profile(arguments))" },
            limit = "ExternalCPSCallMaxCacheSize")
        public void specific(final VirtualFrame frame, final Object target, PorcEClosure pub, Counter counter,
                Terminator term, final Object[] arguments,
                @Cached("getInvokerWithBoundary(target, arguments)") Invoker invoker,
                @Cached("create()") InvokerCanInvoke canInvoke,
                @Cached("create()") InvokerInvoke invoke) {
            // Token: Passed to callContext from arguments.
            final CPSCallContext callContext = new CPSCallContext(execution, pub, counter, term, getCallSiteId());

            try {
                callContext.begin();
                invoke.executeInvoke(frame, invoker, callContext, target, argumentClassesProfile.profile(arguments));
            } catch (final TailCallException e) {
                throw e;
            } catch (final HaltException e) {
                haltProfile.enter();
                counter.haltToken();
            } catch (final Throwable e) {
                exceptionProfile.enter();
                execution.notifyOfException(e, this);
                counter.haltToken();
            } finally {
                if (SpecializationConfiguration.StopWatches.callsEnabled) {
                    orc.run.StopWatches.callTime().stop(FrameUtil.getLongSafe(frame, Call.getCallStartTimeSlot(this)));
                }
            }
        }

        @Specialization(replaces = { "specific", "specificDirect" })
        public void universal(final VirtualFrame frame, final Object target, PorcEClosure pub, Counter counter,
                Terminator term, final Object[] arguments,
                @Cached("createBinaryProfile()") ConditionProfile isDirectProfile,
                @Cached("create()") InvokerInvokeDirect invokeDirect,
                @Cached("create()") InvokerInvoke invoke) {
            final Invoker invoker = getInvokerWithBoundary(target, arguments);
            if (ExternalCPSDirectSpecialization && isDirectProfile.profile(invoker instanceof DirectInvoker)) {
                specificDirect(frame, target, pub, counter, term, arguments, (DirectInvoker) invoker, null, invokeDirect);
            } else {
                specific(frame, target, pub, counter, term, arguments, invoker, null, invoke);
            }
        }

        static ExternalCPSDispatchInternal createBare(PorcEExecution execution) {
            return ExternalCPSDispatchFactory.ExternalCPSDispatchInternalNodeGen.create(execution);
        }

        /* Utilties */

        protected static boolean isNotDirectInvoker(final Invoker invoker) {
            return !(invoker instanceof DirectInvoker);
        }

        protected Invoker getInvokerWithBoundary(final Object target, final Object[] arguments) {
            return getInvokerWithBoundary(execution.runtime(), target, arguments);
        }

        protected DirectInvoker getDirectInvokerWithBoundary(final Object target, final Object[] arguments) {
            Invoker invoker = getInvokerWithBoundary(execution.runtime(), target, arguments);
            if (invoker instanceof DirectInvoker) {
                return (DirectInvoker) invoker;
            } else {
                return null;
            }
        }

        @TruffleBoundary(allowInlining = true)
        protected static Invoker getInvokerWithBoundary(final PorcERuntime runtime, final Object target,
                final Object[] arguments) {
            return runtime.getInvoker(target, arguments);
        }
    }

}
