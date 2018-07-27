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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;

import orc.CallContext;
import orc.DirectInvoker;
import orc.Invoker;
import orc.error.runtime.HaltException;
import orc.run.porce.StackCheckingDispatch;
import orc.run.porce.ValueClassesProfile;
import orc.run.porce.HaltToken;
import orc.run.porce.Logger;
import orc.run.porce.SpecializationConfiguration;
import orc.run.porce.runtime.CPSCallContext;
import orc.run.porce.runtime.Counter;
import orc.run.porce.runtime.DirectVirtualCallContext;
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
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.IntValueProfile;
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
        protected Dispatch dispatchP = null;

        protected Dispatch getDispatchP() {
            if (dispatchP == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                computeAtomicallyIfNull(() -> dispatchP, (v) -> dispatchP = v, () -> {
                    Dispatch n = insert(InternalCPSDispatch.create(/*forceInline =*/ false, execution, isTail));
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
                @Cached("create(execution)") InvokerInvokeDirect invokeDirect,
                @Cached("create(execution)") HaltToken.KnownCounter haltToken) {
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
                getDispatchP().dispatch(frame, pub, v);
            } catch (final TailCallException e) {
                throw e;
            } catch (final HaltException e) {
                haltProfile.enter();
                haltToken.execute(frame, counter);
            } catch (final Throwable e) {
                exceptionProfile.enter();
                execution.notifyOfException(e, this);
                haltToken.execute(frame, counter);
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
                @Cached("create(execution)") InvokerInvoke invoke,
                @Cached("createVirtualContextHandler()") VirtualContextHandler handler) {
            // Token: Passed to callContext from arguments.
            final DirectVirtualCallContext callContext = new DirectVirtualCallContext(execution, getCallSiteId()) {
                @Override
                public PorcEClosure p() {
                    return pub;
                }
                @Override
                public Counter c() {
                    return counter;
                }
                @Override
                public Terminator t() {
                    return term;
                }
            };

            try {
                try {
                    invoke.executeInvoke(frame, invoker, maybeMaterialize(callContext), target, argumentClassesProfile.profile(arguments));
                } finally {
                    if (SpecializationConfiguration.StopWatches.callsEnabled) {
                        orc.run.StopWatches.callTime().stop(FrameUtil.getLongSafe(frame, Call.getCallStartTimeSlot(this)));
                    }
                }
                //Logger.info(() -> target + "(" + Arrays.toString(arguments) + ") -> " + callContext.toString());
            } catch (final TailCallException e) {
                throw e;
            } catch (final HaltException e) {
                haltProfile.enter();
                handler.haltToken.execute(frame, counter);
            } catch (final Throwable e) {
                exceptionProfile.enter();
                execution.notifyOfException(e, this);
                handler.haltToken.execute(frame, counter);
            } finally {
                handler.execute(frame, pub, counter, term, callContext);
            }
        }

        private static CallContext maybeMaterialize(DirectVirtualCallContext callContext) {
            if (SpecializationConfiguration.UseVirtualCallContexts) {
                return callContext;
            } else {
                return callContext.materialize();
            }
        }

        @Specialization(replaces = { "specific", "specificDirect" })
        public void universal(final VirtualFrame frame, final Object target, PorcEClosure pub, Counter counter,
                Terminator term, final Object[] arguments,
                @Cached("createBinaryProfile()") ConditionProfile isDirectProfile,
                @Cached("create(execution)") InvokerInvokeDirect invokeDirect,
                @Cached("create(execution)") InvokerInvoke invoke,
                @Cached("createVirtualContextHandler()") VirtualContextHandler handler) {
            final Invoker invoker = getInvokerWithBoundary(target, arguments);
            if (ExternalCPSDirectSpecialization && isDirectProfile.profile(invoker instanceof DirectInvoker)) {
                specificDirect(frame, target, pub, counter, term, arguments, (DirectInvoker) invoker, null, invokeDirect, handler.haltToken);
            } else {
                specific(frame, target, pub, counter, term, arguments, invoker, null, invoke, handler);
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

        /**
         * Utility node to collect all DirectVirtualCallContext handling code and state.
         *
         * @author amp
         */
        protected final class VirtualContextHandler extends Node {
            private final ConditionProfile exactlyOneSelfPublicationProfile = ConditionProfile.createBinaryProfile();
            private final ConditionProfile hasSelfPublicationsProfile = ConditionProfile.createBinaryProfile();
            private final IntValueProfile selfPublicationsListSizeProfile = IntValueProfile.createIdentityProfile();
            private final ConditionProfile haltedProfile = ConditionProfile.createBinaryProfile();

            private final ConditionProfile hasOtherPublicationsProfile = ConditionProfile.createBinaryProfile();
            private final IntValueProfile otherPublicationsListSizeProfile = IntValueProfile.createIdentityProfile();
            private final ConditionProfile otherPublicationsHaltProfile = ConditionProfile.createBinaryProfile();

            private final ConditionProfile hasOtherHalts = ConditionProfile.createBinaryProfile();
            private final IntValueProfile otherHaltsListSizeProfile = IntValueProfile.createIdentityProfile();

            @Child
            HaltToken.KnownCounter haltToken = HaltToken.KnownCounter.create(execution);

            @SuppressWarnings("null")
            public void execute(VirtualFrame frame, PorcEClosure pub, Counter counter, Terminator term, DirectVirtualCallContext callContext) {
                final boolean halted = callContext.halted();
                final ArrayList<Object> selfPublicationList = callContext.selfPublicationList();
                final ArrayList<Object> otherPublicationList = callContext.otherPublicationList();
                final ArrayList<CPSCallContext> otherHaltedList = callContext.otherHaltedList();
                callContext.close();

                if (hasOtherPublicationsProfile.profile(otherPublicationList != null)) {
                    final int size = otherPublicationsListSizeProfile.profile(otherPublicationList.size());
                    for (int i = 0; i < size; i += 3) {
                        CPSCallContext ctx = (CPSCallContext)otherPublicationList.get(i + 0);
                        Object v = otherPublicationList.get(i + 1);
                        boolean halt = ((Boolean)otherPublicationList.get(i + 2)).booleanValue();
                        if (!otherPublicationsHaltProfile.profile(halt)) {
                            //throw new RuntimeException();
                            ctx.c().newToken();
                        }
                        getDispatchP().dispatch(frame, ctx.p(), v);
                        if (otherPublicationsHaltProfile.profile(halt)) {
                            ctx.t().removeChild(ctx);
                        }
                    }
                }

                if (hasOtherHalts.profile(otherHaltedList != null)) {
                    //throw new RuntimeException();
                    final int size = otherHaltsListSizeProfile.profile(otherHaltedList.size());
                    for (int i = 0; i < size; i++) {
                        CPSCallContext ctx = otherHaltedList.get(i);
                        haltToken.execute(frame, ctx.c());
                        ctx.t().removeChild(ctx);
                    }
                }

                if (exactlyOneSelfPublicationProfile.profile(selfPublicationList != null && selfPublicationList.size() == 1 && halted)) {
                    getDispatchP().dispatch(frame, pub, selfPublicationList.get(0));
                } else {
                    if (hasSelfPublicationsProfile.profile(selfPublicationList != null)) {
                        final int size = selfPublicationsListSizeProfile.profile(selfPublicationList.size());
                        for (int i = 0; i < size; i++) {
                            //throw new RuntimeException();
                            counter.newToken();
                            getDispatchP().dispatch(frame, pub, selfPublicationList.get(i));
                        }
                    }
                    if (haltedProfile.profile(halted)) {
                        haltToken.execute(frame, counter);
                    }
                }

            }
        }

        protected VirtualContextHandler createVirtualContextHandler() {
            return new VirtualContextHandler();
        }
    }

}
