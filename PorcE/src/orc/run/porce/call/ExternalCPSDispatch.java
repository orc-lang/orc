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
import orc.SiteResponseSet;
import orc.error.runtime.HaltException;
import orc.run.porce.HaltToken;
import orc.run.porce.NodeBase;
import orc.run.porce.SpecializationConfiguration;
import orc.run.porce.StackCheckingDispatch;
import orc.run.porce.profiles.ValueClassesProfile;
import orc.run.porce.runtime.CallContextCommon;
import orc.run.porce.runtime.Counter;
import orc.run.porce.runtime.MaterializedCPSCallContext;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecution;
import orc.run.porce.runtime.PorcERuntime;
import orc.run.porce.runtime.ResponseSet;
import orc.run.porce.runtime.TailCallException;
import orc.run.porce.runtime.Terminator;
import orc.run.porce.runtime.VirtualCPSCallContext;

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
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;

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

        private static final Object SENTINAL = new Object();

        protected final BranchProfile exceptionProfile = BranchProfile.create();
        protected final BranchProfile haltProfile = BranchProfile.create();

        protected final ValueClassesProfile argumentClassesProfile = new ValueClassesProfile();

//        @CompilerDirectives.CompilationFinal
//        protected Dispatch dispatchP = null;
//
//        protected Dispatch getDispatchP() {
//            if (dispatchP == null) {
//                CompilerDirectives.transferToInterpreterAndInvalidate();
//                computeAtomicallyIfNull(() -> dispatchP, (v) -> dispatchP = v, () -> {
//                    Dispatch n = insert();
//                    notifyInserted(n);
//                    return n;
//                });
//            }
//            return dispatchP;
//        }

        protected Dispatch createDispatchP() {
            return StackCheckingDispatch.create(execution);
            // return InternalCPSDispatch.create(/*forceInline =*/ true, execution, isTail);
        }

        @SuppressWarnings("hiding")
        protected ResponseSetHandler createResponseSetHandler(final PorcEExecution execution) {
            return ExternalCPSDispatchFactory.ResponseSetHandlerNodeGen.create(execution);
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
                @Cached("createDispatchP()") Dispatch dispatchP,
                @Cached("create(execution)") HaltToken.KnownCounter haltToken) {
            ensureTail(dispatchP);
            ensureForceInline(dispatchP);
            ensureTail(haltToken);
            // DUPLICATION: This code is duplicated (mostly) in ExternalDirectDispatch.specific.
            Object v = SENTINAL;
            try {
                try {
                    v = invokeDirect.executeInvokeDirect(frame, invoker, target, argumentClassesProfile.profile(arguments));
                } finally {
                    if (SpecializationConfiguration.StopWatches.callsEnabled) {
                        orc.run.StopWatches.callTime()
                                .stop(FrameUtil.getLongSafe(frame, Call.getCallStartTimeSlot(this)));
                    }
                }
            } catch (final TailCallException e) {
                throw e;
            } catch (final HaltException e) {
                haltProfile.enter();
                v = SENTINAL;
                execution.notifyOfException(e, this);
                haltToken.execute(frame, counter);
            } catch (final Throwable e) {
                exceptionProfile.enter();
                v = SENTINAL;
                execution.notifyOfException(e, this);
                haltToken.execute(frame, counter);
            }
            if (v != SENTINAL) {
                dispatchP.dispatch(frame, pub, v);
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
                @Cached("createResponseSetHandler(execution)") ResponseSetHandler handler) {
            ensureTail(handler);
            // Token: Passed to callContext from arguments.
            final VirtualCPSCallContext callContext = new VirtualCPSCallContext(execution, pub, counter, term, getCallSiteId());

            SiteResponseSet rs = null;
            try {
                try {
                    rs = invoke.executeInvoke(frame, invoker, callContext, target, argumentClassesProfile.profile(arguments));
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
                execution.notifyOfException(e, this);
                handler.haltToken.execute(frame, counter);
            } catch (final Throwable e) {
                exceptionProfile.enter();
                execution.notifyOfException(e, this);
                handler.haltToken.execute(frame, counter);
            } finally {
                if (rs != null) {
                    handler.execute(frame, rs);
                }
            }
        }

//        private static CallContext maybeMaterialize(VirtualCPSCallContext callContext) {
//            if (SpecializationConfiguration.UseVirtualCallContexts) {
//                return callContext;
//            } else {
//                return callContext.materialize();
//            }
//        }

        @Specialization(replaces = { "specific", "specificDirect" })
        public void universal(final VirtualFrame frame, final Object target, PorcEClosure pub, Counter counter,
                Terminator term, final Object[] arguments,
                @Cached("createBinaryProfile()") ConditionProfile isDirectProfile,
                @Cached("create(execution)") InvokerInvokeDirect invokeDirect,
                @Cached("create(execution)") InvokerInvoke invoke,
                @Cached("createResponseSetHandler(execution)") ResponseSetHandler handler,
                @Cached("createDispatchP()") Dispatch dispatchP,
                @Cached("create(execution)") HaltToken.KnownCounter haltToken) {
            final Invoker invoker = getInvokerWithBoundary(target, arguments);
            if (ExternalCPSDirectSpecialization && isDirectProfile.profile(invoker instanceof DirectInvoker)) {
                specificDirect(frame, target, pub, counter, term, arguments, (DirectInvoker) invoker, null, invokeDirect, dispatchP, haltToken);
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
    }

    /**
     * Utility node to collect all DirectVirtualCallContext handling code and state.
     *
     * @author amp
     */
    @ImportStatic({ ExternalCPSDispatch.class })
    public static abstract class ResponseSetHandler extends NodeBase {
        protected static final ResponseSet EMPTY = ResponseSet.Empty$.MODULE$;

        protected final PorcEExecution execution;

        protected ResponseSetHandler(final PorcEExecution execution) {
            this.execution = execution;
            this.dispatchP = InternalCPSDispatch.create(execution, false);
            this.haltToken = HaltToken.KnownCounter.create(execution);
        }

        @Child
        protected Dispatch dispatchP;

        @Child
        protected HaltToken.KnownCounter haltToken;

        @Child
        protected ResponseSetHandler next = null;

        protected ResponseSetHandler getNext() {
          if (next == null) {
              CompilerDirectives.transferToInterpreterAndInvalidate();
              computeAtomicallyIfNull(() -> next, (v) -> next = v, () -> {
                  ResponseSetHandler n = insert(ExternalCPSDispatchFactory.ResponseSetHandlerNodeGen.create(execution));
                  notifyInserted(n);
                  return n;
              });
          }
          return next;
        }

        public abstract void execute(VirtualFrame frame, SiteResponseSet rs);

        @Specialization(guards = { "rs == EMPTY" })
        public void empty(VirtualFrame frame, ResponseSet rs) {
            // Noop
        }

        @Specialization
        public void publishNonterminal(VirtualFrame frame, ResponseSet.PublishNonterminal rs) {
            CallContextCommon ctx = (CallContextCommon)rs.ctx();
            ctx.c().newToken();
            dispatchP.dispatch(frame, ctx.p(), rs.v());
            if (rs.next() != null) {
                getNext().execute(frame, rs.next());
            }
        }

        @Specialization
        public void publishTerminal(VirtualFrame frame, ResponseSet.PublishTerminal rs) {
            CallContextCommon ctx = (CallContextCommon)rs.ctx();
            dispatchP.dispatch(frame, ctx.p(), rs.v());
            if (ctx instanceof MaterializedCPSCallContext) {
                ctx.t().removeChild((MaterializedCPSCallContext)ctx);
            }
            if (rs.next() != null) {
                getNext().execute(frame, rs.next());
            }
        }

        @Specialization
        public void halt(VirtualFrame frame, ResponseSet.Halt rs) {
            CallContextCommon ctx = (CallContextCommon)rs.ctx();
            haltToken.execute(frame, ctx.c());
            if (ctx instanceof MaterializedCPSCallContext) {
                ctx.t().removeChild((MaterializedCPSCallContext)ctx);
            }
            if (rs.next() != null) {
                getNext().execute(frame, rs.next());
            }
        }

        @Specialization
        public void discorporate(VirtualFrame frame, ResponseSet.Discorporate rs) {
            CallContextCommon ctx = (CallContextCommon)rs.ctx();
            ctx.c().setDiscorporate();
            haltToken.execute(frame, ctx.c());
            if (ctx instanceof MaterializedCPSCallContext) {
                ctx.t().removeChild((MaterializedCPSCallContext)ctx);
            }
            if (rs.next() != null) {
                getNext().execute(frame, rs.next());
            }
        }
    }
}
