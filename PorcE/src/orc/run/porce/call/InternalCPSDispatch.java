//
// InternalCPSDispatch.java -- Java class InternalCPSDispatch
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.call;

import java.util.function.BiConsumer;

import orc.error.runtime.ArityMismatchException;
import orc.run.porce.PorcERootNode;
import orc.run.porce.SpecializationConfiguration;
import orc.run.porce.runtime.CallKindDecision;
import orc.run.porce.runtime.InlinedTailCallException;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecution;
import orc.run.porce.runtime.SelfTailCallException;
import orc.run.porce.runtime.TailCallException;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Introspectable;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Instrumentable;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;

@Instrumentable(factory = InternalCPSDispatchWrapper.class)
public class InternalCPSDispatch extends Dispatch {
    @Child
    protected InternalCPSDispatch.InternalCPSDispatchInternal internal;

    protected InternalCPSDispatch(final PorcEExecution execution) {
        super(execution);
        internal = InternalCPSDispatch.InternalCPSDispatchInternal.createBare(execution);
    }

    protected InternalCPSDispatch(final InternalCPSDispatch orig) {
        super(orig.internal.execution);
        internal = InternalCPSDispatch.InternalCPSDispatchInternal.createBare(orig.internal.execution);
        orig.ensureForceInline(this);
        ensureForceInline(this);
    }

    @Override
    public void setTail(boolean v) {
        super.setTail(v);
        internal.setTail(v);
    }

    @Override
    public void executeDispatchWithEnvironment(VirtualFrame frame, Object target, Object[] arguments) {
        long startTime = getProfilingScope().getTime();
        arguments[0] = ((PorcEClosure) target).environment;
        internal.execute(frame, target, arguments);
        getProfilingScope().removeTime(startTime);
    }

    @Override
    public void executeDispatch(VirtualFrame frame, Object target, Object[] arguments) {
        long startTime = getProfilingScope().getTime();
        internal.execute(frame, target, buildArguments((PorcEClosure) target, arguments));
        getProfilingScope().removeTime(startTime);
    }

    protected static Object[] buildArguments(PorcEClosure target, Object[] arguments) {
        // CompilerAsserts.compilationConstant(arguments.length);
        Object[] newArguments = new Object[arguments.length + 1];
        newArguments[0] = target.environment;
        System.arraycopy(arguments, 0, newArguments, 1, arguments.length);
        return newArguments;
    }

    static InternalCPSDispatch createBare(PorcEExecution execution) {
        return new InternalCPSDispatch(execution);
    }

    public static Dispatch create(final PorcEExecution execution, boolean isTail) {
        if (isTail || !SpecializationConfiguration.UniversalTCO) {
            return createBare(execution);
        } else {
            return CatchTailDispatch.create(createBare(execution), execution);
        }
    }

    @ImportStatic({ SpecializationConfiguration.class })
    @Introspectable
    @Instrumentable(factory = InternalCPSDispatchInternalWrapper.class)
    public static abstract class InternalCPSDispatchInternal extends DispatchBase {
        protected InternalCPSDispatchInternal(final PorcEExecution execution) {
            super(execution);
        }

        protected InternalCPSDispatchInternal(final InternalCPSDispatchInternal orig) {
            super(orig.execution);
            orig.ensureForceInline(this);
        }

        public int computeNodeCount() {
            CompilerAsserts.neverPartOfCompilation();
            return NodeUtil.countNodes(getRootNode());
        }

        protected final BranchProfile exceptionProfile = BranchProfile.create();
        protected final BranchProfile haltProfile = BranchProfile.create();

        public abstract void execute(VirtualFrame frame, Object target, Object[] arguments);

        // Tail calls

        @SuppressWarnings("boxing")
        @Specialization(guards = { "SelfTCO", "isTail", "getCachedRootNode() == target.body.getRootNode()" })
        public void selfTail(final VirtualFrame frame, final PorcEClosure target, final Object[] arguments) {
            addCalledRoot(target.body);
            Object[] frameArguments = frame.getArguments();
            CompilerAsserts.compilationConstant(arguments.length);
            if (frameArguments.length == arguments.length) {
                for (int i = 0; i < arguments.length; i ++) {
                    //CompilerDirectives.ensureVirtualizedHere(arguments);
                    frameArguments[i] = arguments[i];
                }
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new ArityMismatchException(frameArguments.length - 3, arguments.length - 3);
            }
            //Logger.log(java.util.logging.Level.INFO, () -> "Self tail call: " + target.toString() + " (" + java.util.Arrays.toString(arguments) + ")");
            throw new SelfTailCallException();
        }

        protected BiConsumer<VirtualFrame, Object[]> findInlinedCallRoot(final RootCallTarget target) {
            InlinedCallRoot node = InlinedCallRoot.findInlinedCallRoot(this, target.getRootNode());
            if (node == null) {
                return null;
            } else {
                return node::copyArgumentsToFrame;
            }
        }

        @Specialization(guards = { "isTail", "copyArgumentsToFrame != null",
                "getCachedRootNode() != target.body.getRootNode()" })
        public void inlinedTail(final VirtualFrame frame,
                final PorcEClosure target, final Object[] arguments,
                @Cached("target.body") RootCallTarget expected,
                @Cached("findInlinedCallRoot(expected)") BiConsumer<VirtualFrame, Object[]> copyArgumentsToFrame) {
            addCalledRoot(target.body);
            copyArgumentsToFrame.accept(frame, arguments);
            throw new InlinedTailCallException((PorcERootNode)expected.getRootNode());
        }

        // The RootNode guard is required so that selfTail can be activated even
        // after tail has activated.
        @Specialization(guards = { "UniversalTCO", "isTail",
                "getCachedRootNode() != target.body.getRootNode()" })
        public void universalTail(final VirtualFrame frame, final PorcEClosure target, final Object[] arguments,
                @Cached("createBinaryProfile()") ConditionProfile reuseTCE) {
            addCalledRoot(target.body);
            Object[] thisArguments = frame.getArguments();
            if (reuseTCE.profile(/* arguments.length <= 16 && */ thisArguments.length == 17
                    && thisArguments[16] instanceof TailCallException)) {
                TailCallException tce = (TailCallException) thisArguments[16];
                System.arraycopy(arguments, 0, tce.arguments, 0, arguments.length);
                tce.target = target;
                throw tce;
            }

            throw TailCallException.create(target, arguments);
        }

        // Non-tail calls

        protected boolean canSpecificInlineAST(RootCallTarget target) {
            return (getCachedRootNode() instanceof PorcERootNode);
        }

        protected boolean oneTimeGuard_SpecificInlineAST(VirtualFrame frame,
                RootCallTarget target, InlinedCallRoot inlinedCall) {
            CompilerAsserts.neverPartOfCompilation();
            if (inlinedCall == null || !(target.getRootNode() instanceof PorcERootNode)) {
                return false;
            }
            CallKindDecision decision = CallKindDecision.get(this, (PorcERootNode)target.getRootNode());
            int nodeCount = computeNodeCount();
            boolean inlineGuess =
                    InlinedCallRoot.isInMethod(this, target.getRootNode()) &&
                    nodeCount < SpecializationConfiguration.TruffleASTInliningLimit;
            return inlinedCall.isApplicable(frame) &&
                    InlinedCallRoot.findInlinedCallRoot(this, target.getRootNode()) == null &&
                    (decision == CallKindDecision.INLINE ||
                        decision == CallKindDecision.ANY && inlineGuess);
        }

        @SuppressWarnings("hiding")
        protected InlinedCallRoot createInlinedCallRoot(final RootNode targetRootNode, final PorcEExecution execution) {
            if (targetRootNode instanceof PorcERootNode) {
                return InlinedCallRoot.create(targetRootNode, execution);
            } else {
                return null;
            }
        }

        // TODO: Use inliningForced.
        @Specialization(guards = { "TruffleASTInlining",
                "target.body == expected",
                "getCachedRootNode() != target.body.getRootNode()",
                "canSpecificInlineAST(expected)",
                "oneTimeGuard"}, limit = "3")
        public void specificInlineAST(final VirtualFrame frame,
                final PorcEClosure target, final Object[] arguments,
                @Cached("target.body") RootCallTarget expected,
                @Cached("createInlinedCallRoot(expected.getRootNode(), execution)") InlinedCallRoot inlinedCall,
                @Cached("oneTimeGuard_SpecificInlineAST(frame, expected, inlinedCall)") boolean oneTimeGuard) {
            ensureTail(inlinedCall);
            addCalledRoot(expected);
            inlinedCall.execute(frame, arguments);
        }

        @Specialization(guards = { "target.body == expected" },
                limit = "InternalCallMaxCacheSize")
        public void specific(final VirtualFrame frame,
                final PorcEClosure target, final Object[] arguments,
                @Cached("target.body") RootCallTarget expected,
                @Cached("create(expected)") DirectCallNode call) {
            addCalledRoot(call.getCurrentCallTarget());

            try {
                call.call(arguments);
            } catch (final TailCallException e) {
                if (!SpecializationConfiguration.UniversalTCO) {
                    CompilerDirectives.transferToInterpreter();
                }
                throw e;
            }
        }

        @Specialization(replaces = { "specific", "specificInlineAST" })
        public void universal(final VirtualFrame frame, final PorcEClosure target, final Object[] arguments,
                @Cached("create()") IndirectCallNode call) {
            addCalledRoot(target.body);
            try {
                call.call(target.body, arguments);
            } catch (final TailCallException e) {
                if (!SpecializationConfiguration.UniversalTCO) {
                    CompilerDirectives.transferToInterpreter();
                }
                throw e;
            }
        }

        static InternalCPSDispatchInternal createBare(final PorcEExecution execution) {
            return InternalCPSDispatchFactory.InternalCPSDispatchInternalNodeGen.create(execution);
        }
    }
}
