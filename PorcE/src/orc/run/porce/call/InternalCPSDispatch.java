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

import orc.error.runtime.ArityMismatchException;
import orc.run.porce.Expression;
import orc.run.porce.Logger;
import orc.run.porce.PorcERootNode;
import orc.run.porce.SpecializationConfiguration;
import orc.run.porce.runtime.InlinedTailCallException;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecution;
import orc.run.porce.runtime.SelfTailCallException;
import orc.run.porce.runtime.TailCallException;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Introspectable;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
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

        @CompilationFinal
        private RootNode rootNode = null;

//        public RootNode getRootNodeCached() {
//            if (rootNode == null || CompilerDirectives.inInterpreter()) {
//                rootNode = getRootNode();
//            }
//            return rootNode;
//        }

        public int computeNodeCount() {
            return NodeUtil.countNodes(getRootNode());
        }

        protected final BranchProfile exceptionProfile = BranchProfile.create();
        protected final BranchProfile haltProfile = BranchProfile.create();

        public abstract void execute(VirtualFrame frame, Object target, Object[] arguments);

        // Tail calls

        @SuppressWarnings("boxing")
        @Specialization(guards = { "SelfTCO", "isTail", "getRootNode() == target.body.getRootNode()" })
        public void selfTail(final VirtualFrame frame, final PorcEClosure target, final Object[] arguments) {
            addCalledRoot(target.body);
            Object[] frameArguments = frame.getArguments();
            //CompilerAsserts.compilationConstant(frameArguments.length);
            CompilerAsserts.compilationConstant(arguments.length);
            if (frameArguments.length == arguments.length) {
                for (int i = 0; i < arguments.length; i ++) {
                    frameArguments[i] = arguments[i];
                }
                //System.arraycopy(arguments, 0, frameArguments, 0, arguments.length);
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new ArityMismatchException(arguments.length, frameArguments.length);
            }
            //Logger.log(java.util.logging.Level.INFO, () -> "Self tail call: " + target.toString() + " (" + java.util.Arrays.toString(arguments) + ")");
            throw new SelfTailCallException();
        }

        @Specialization(guards = { "isTail", "isAbove(expected)", "getRootNode() != target.body.getRootNode()" })
        public void inlinedTail(final VirtualFrame frame,
                final PorcEClosure target, final Object[] arguments,
                @Cached("target.body") RootCallTarget expected) {
            addCalledRoot(target.body);
            throw new InlinedTailCallException((PorcERootNode)expected.getRootNode(), arguments);
        }

        // The RootNode guard is required so that selfTail can be activated even
        // after tail has activated.
        @Specialization(guards = { "UniversalTCO", "isTail", "getRootNode() != target.body.getRootNode()" })
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

        protected boolean sameMethod(RootCallTarget expected) {
            boolean sameMethod = getRootNode() instanceof PorcERootNode &&
                    expected.getRootNode() instanceof PorcERootNode &&
                    ((PorcERootNode)getRootNode()).getMethodKey() == ((PorcERootNode)expected.getRootNode()).getMethodKey();
            return sameMethod;
        }

        @Specialization(guards = { "TruffleASTInlining", "isTail",
                "nodeCount < TruffleASTInliningLimit",
                "target.body == expected", "!isAbove(expected)", "sameMethod(expected)",
                "getRootNode() != target.body.getRootNode()",
                "canSpecificInlineAST()",
                "isApplicable"}, limit = "3")
        public void specificInlineAST(final VirtualFrame frame,
                final PorcEClosure target, final Object[] arguments,
                @Cached("target.body") RootCallTarget expected,
                @Cached("computeNodeCount()") int nodeCount,
                @Cached("createInlinedCallRoot(expected.getRootNode(), execution)") InlinedCallRoot inlinedCall,
                @Cached("inlinedCall.isApplicable(frame)") boolean isApplicable) {
            ensureTail(inlinedCall);
            addCalledRoot(expected);
            CompilerDirectives.ensureVirtualized(arguments);
            inlinedCall.execute(frame, arguments);
        }

        protected InlinedCallRoot createInlinedCallRoot(final RootNode targetRootNode, final PorcEExecution execution) {
            return InlinedCallRoot.create(targetRootNode, execution);
        }

        protected boolean canSpecificInlineAST() {
            return (getRootNode() instanceof PorcERootNode) &&
                    !getRootNode().isInternal();
        }
        protected boolean isAbove(final RootCallTarget target) {
            boolean b = InlinedCallRoot.isAbove(this, target.getRootNode());
//            Logger.info(() -> String.format("%s, %b",
//                    target.getRootNode(), b));
            return b;
        }

//        @Specialization(guards = { "TruffleASTInlining", "nodeCount < TruffleASTInliningLimit",
//                "target.body == expected", "sameMethod(expected)", "body != null",
//                "getRootNode() != target.body.getRootNode()" }, limit = "3")
        public void specificInline(final VirtualFrame frame,
                final PorcEClosure target, final Object[] arguments,
                @Cached("target.body") RootCallTarget expected,
                @Cached("getPorcEBodyNewFrame(target)") Expression body,
                @Cached("getPorcEFrameDescriptor(target)") FrameDescriptor fd,
                @Cached("computeNodeCount()") int nodeCount) {
            ensureTail(body);
            addCalledRoot(expected);
            CompilerDirectives.ensureVirtualized(arguments);
            try {
                final VirtualFrame nestedFrame = Truffle.getRuntime().createVirtualFrame(arguments, fd);
                body.execute(nestedFrame);
//            } catch(NullPointerException e) {
//                CompilerDirectives.transferToInterpreter();
//                Logger.log(java.util.logging.Level.WARNING, () -> "UGLY HACK: Creating virtual frame failed. Hopefully this is just as the program exits.", e);
            } catch (final SelfTailCallException e) {
                if (!isTail) {
                    CompilerDirectives.transferToInterpreter();
                    Logger.log(java.util.logging.Level.WARNING, () -> "Caught SelfTailCallException at non-tail specificInline", e);
                }
                throw e;
            } catch (final TailCallException e) {
                if (!SpecializationConfiguration.UniversalTCO) {
                    CompilerDirectives.transferToInterpreter();
                }
                throw e;
            }
        }

        @Specialization(guards = { "target.body == expected" },
                limit = "InternalCallMaxCacheSize")
        public void specific(final VirtualFrame frame,
                final PorcEClosure target, final Object[] arguments,
                @Cached("target.body") RootCallTarget expected,
                @Cached("create(expected)") DirectCallNode call) {
            addCalledRoot(call.getCurrentCallTarget());
            CompilerDirectives.interpreterOnly(() -> {
                if (sameMethod(expected) || inliningForced) {
                    call.forceInlining();
                }
            });

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

        /* Utilties */

        protected Expression getPorcEBodyNewFrame(PorcEClosure target) {
            CompilerAsserts.neverPartOfCompilation("Copying PorcE AST.");
            RootNode r = target.body.getRootNode();
            if (r instanceof PorcERootNode) {
                PorcERootNode bodyRoot = (PorcERootNode) r;

                Expression body = bodyRoot.getBody();
                Expression newBody = NodeUtil.cloneNode(body);

                newBody.setTail(isTail);

                return newBody;
            } else {
                return null;
            }
        }

        protected static FrameDescriptor getPorcEFrameDescriptor(PorcEClosure target) {
            RootNode r = target.body.getRootNode();
            if (r instanceof PorcERootNode) {
                return r.getFrameDescriptor();
            } else {
                return null;
            }
        }

        static InternalCPSDispatchInternal createBare(final PorcEExecution execution) {
            return InternalCPSDispatchFactory.InternalCPSDispatchInternalNodeGen.create(execution);
        }
}
}
