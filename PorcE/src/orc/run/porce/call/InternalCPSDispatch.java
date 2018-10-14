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

import scala.collection.Seq;

import orc.ast.porc.Variable;
import orc.compiler.porce.PorcToPorcE;
import orc.error.runtime.ArityMismatchException;
import orc.error.runtime.HaltException;
import orc.run.porce.Expression;
import orc.run.porce.Logger;
import orc.run.porce.PorcERootNode;
import orc.run.porce.SpecializationConfiguration;
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
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Instrumentable;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.ExplodeLoop;
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
        arguments[0] = ((PorcEClosure) target).environment;
        internal.execute(frame, target, arguments);
    }

    @Override
    public void executeDispatch(VirtualFrame frame, Object target, Object[] arguments) {
        internal.execute(frame, target, buildArguments((PorcEClosure) target, arguments));
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

        public RootNode getRootNodeCached() {
            if (rootNode == null || CompilerDirectives.inInterpreter()) {
                rootNode = getRootNode();
            }
            return rootNode;
        }

        public int computeNodeCount() {
            return NodeUtil.countNodes(getRootNodeCached());
        }

        protected final BranchProfile exceptionProfile = BranchProfile.create();
        protected final BranchProfile haltProfile = BranchProfile.create();

        public abstract void execute(VirtualFrame frame, Object target, Object[] arguments);

        // TODO: It would probably improve compile times to split tail and non-tail
        // cases into separate classes so only one set has to be checked for any call.

        // Tail calls

        @Specialization(guards = { "SelfTCO", "isTail", "getRootNodeCached() == target.body.getRootNode()" })
        public void selfTail(final VirtualFrame frame, final PorcEClosure target, final Object[] arguments) {
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

        // The RootNode guard is required so that selfTail can be activated even
        // after tail has activated.
        @Specialization(guards = { "UniversalTCO", "isTail", "getRootNodeCached() != target.body.getRootNode()" })
        public void tail(final VirtualFrame frame, final PorcEClosure target, final Object[] arguments,
                @Cached("createBinaryProfile()") ConditionProfile reuseTCE) {
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
            boolean sameMethod = getRootNodeCached() instanceof PorcERootNode &&
                    expected.getRootNode() instanceof PorcERootNode &&
                    ((PorcERootNode)getRootNodeCached()).getMethodKey() == ((PorcERootNode)expected.getRootNode()).getMethodKey();
            return sameMethod;
        }

        @Specialization(guards = { "TruffleASTInlining", "isTail",
                "nodeCount < TruffleASTInliningLimit",
                "target.body == expected",
                "body != null", "argumentSlots != null", "closureSlots != null",
                "getRootNodeCached() != target.body.getRootNode()" }, limit = "3")
        @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.FULL_UNROLL)
        public void specificInlineTail(final VirtualFrame frame,
                final PorcEClosure target, final Object[] arguments,
                @Cached("target.body") RootCallTarget expected,
                @Cached("getPorcEBodyFrameArguments(target, frame)") Expression body,
                @Cached(value = "getArgumentSlots(target)", dimensions = 1) FrameSlot[] argumentSlots,
                @Cached(value = "getClosureSlots(target)", dimensions = 1) FrameSlot[] closureSlots,
                @Cached("computeNodeCount()") int nodeCount) {
            ensureTail(body);
            CompilerDirectives.ensureVirtualized(arguments);
            try {
                if (arguments.length != argumentSlots.length + 1) {
                    CompilerDirectives.transferToInterpreter();
                    throw new ArityMismatchException(arguments.length - 1, argumentSlots.length);
                }

                for (int i = 0; i < argumentSlots.length; i++) {
                    FrameSlot slot = argumentSlots[i];
                    frame.setObject(slot, arguments[i+1]);
                }

                for (int i = 0; i < closureSlots.length; i++) {
                    FrameSlot slot = closureSlots[i];
                    frame.setObject(slot, ((Object[]) arguments[0])[i]);
                }

                body.execute(frame);
            } catch (final SelfTailCallException e) {
                throw e;
            } catch (final TailCallException e) {
                if (!SpecializationConfiguration.UniversalTCO) {
                    CompilerDirectives.transferToInterpreter();
                }
                throw e;
            } catch (final HaltException e) {
                haltProfile.enter();
                throw e;
            } catch (final Throwable e) {
                exceptionProfile.enter();
                execution.notifyOfException(e, this);
                throw new HaltException();
            }
        }

//        @Specialization(guards = { "TruffleASTInlining", "nodeCount < TruffleASTInliningLimit",
//                "target.body == expected", "sameMethod(expected)", "body != null",
//                "getRootNodeCached() != target.body.getRootNode()" }, limit = "3")
        public void specificInline(final VirtualFrame frame,
                final PorcEClosure target, final Object[] arguments,
                @Cached("target.body") RootCallTarget expected,
                @Cached("getPorcEBodyNewFrame(target)") Expression body,
                @Cached("getPorcEFrameDescriptor(target)") FrameDescriptor fd,
                @Cached("computeNodeCount()") int nodeCount) {
            ensureTail(body);
            CompilerDirectives.ensureVirtualized(arguments);
            try {
                final VirtualFrame nestedFrame = Truffle.getRuntime().createVirtualFrame(arguments, fd);
                body.execute(nestedFrame);
            } catch(NullPointerException e) {
                CompilerDirectives.transferToInterpreter();
                Logger.log(java.util.logging.Level.WARNING, () -> "UGLY HACK: Creating virtual frame failed. Hopefully this is just as the program exits.", e);
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
            } catch (final HaltException e) {
                haltProfile.enter();
                throw e;
            } catch (final Throwable e) {
                exceptionProfile.enter();
                execution.notifyOfException(e, this);
                throw new HaltException();
            }
        }

        @Specialization(guards = { "target.body == expected" },
                limit = "InternalCallMaxCacheSize")
        public void specific(final VirtualFrame frame,
                final PorcEClosure target, final Object[] arguments,
                @Cached("target.body") RootCallTarget expected,
                @Cached("create(expected)") DirectCallNode call) {
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
            } catch (final HaltException e) {
                haltProfile.enter();
                throw e;
            } catch (final Throwable e) {
                exceptionProfile.enter();
                execution.notifyOfException(e, this);
                throw new HaltException();
            }
        }

        @Specialization(replaces = { "specific", "specificInlineTail" })
        public void universal(final VirtualFrame frame, final PorcEClosure target, final Object[] arguments,
                @Cached("create()") IndirectCallNode call) {
            try {
                call.call(target.body, arguments);
            } catch (final TailCallException e) {
                if (!SpecializationConfiguration.UniversalTCO) {
                    CompilerDirectives.transferToInterpreter();
                }
                throw e;
            } catch (final HaltException e) {
                haltProfile.enter();
                throw e;
            } catch (final Throwable e) {
                exceptionProfile.enter();
                execution.notifyOfException(e, this);
                throw new HaltException();
            }
        }

        /* Utilties */

        protected FrameSlot[] getArgumentSlots(PorcEClosure target) {
            CompilerAsserts.neverPartOfCompilation("Frame update");
            RootNode r = target.body.getRootNode();
            if (getRootNode() instanceof PorcERootNode && r instanceof PorcERootNode) {
                PorcERootNode myRoot = (PorcERootNode) getRootNode();
                PorcERootNode bodyRoot = (PorcERootNode) r;

                Seq<Variable> argumentVariables = bodyRoot.getArgumentVariables();
                FrameDescriptor descriptor = myRoot.getFrameDescriptor();
                FrameSlot[] res = new FrameSlot[argumentVariables.size()];

                for (int i = 0; i < res.length; i++) {
                    Variable x = argumentVariables.apply(i);
                    res[i] = descriptor.findOrAddFrameSlot(x, FrameSlotKind.Object);
                }
                return res;
            } else {
                return null;
            }
        }

        protected FrameSlot[] getClosureSlots(PorcEClosure target) {
            CompilerAsserts.neverPartOfCompilation("Frame update");
            RootNode r = target.body.getRootNode();
            if (getRootNode() instanceof PorcERootNode && r instanceof PorcERootNode) {
                PorcERootNode myRoot = (PorcERootNode) getRootNode();
                PorcERootNode bodyRoot = (PorcERootNode) r;

                Seq<Variable> closureVariables = bodyRoot.getClosureVariables();
                FrameDescriptor descriptor = myRoot.getFrameDescriptor();
                FrameSlot[] res = new FrameSlot[closureVariables.size()];

                for (int i = 0; i < res.length; i++) {
                    Variable x = closureVariables.apply(i);
                    res[i] = descriptor.findOrAddFrameSlot(x, FrameSlotKind.Object);
                }
                return res;
            } else {
                return null;
            }
        }

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

        protected Expression getPorcEBodyFrameArguments(PorcEClosure target, VirtualFrame frame) {
            CompilerAsserts.neverPartOfCompilation("Reconvertion of code.");
            RootNode r = target.body.getRootNode();
            if (getRootNode() instanceof PorcERootNode && r instanceof PorcERootNode) {
                PorcERootNode myRoot = (PorcERootNode) getRootNode();
                PorcERootNode bodyRoot = (PorcERootNode) r;
                Expression body = bodyRoot.getBody();

                if (body.porcNode().isEmpty() ||
                        myRoot.getArgumentVariables() == null || bodyRoot.getArgumentVariables() == null) {
                    return null;
                }

                // Now reconvert the original Porc code to PorcE in a new context.
                Expression res = orc.compiler.porce.PorcToPorcE.expression(
                        (orc.ast.porc.Expression)body.porcNode().get(),
                        PorcToPorcE.variableSeq(),
                        PorcToPorcE.variableSeq(),
                        frame.getFrameDescriptor(),
                        execution.callTargetMap(),
                        scala.collection.immutable.Map$.MODULE$.empty(),
                        execution,
                        execution.runtime().language(),
                        isTail
                        );

//                Logger.info(() -> {
//                    String bodyStr = NodeUtil.printTreeToString(body);
//                    String resStr = NodeUtil.printTreeToString(res);
//                    return "AST inlining " + target + " at " + this + ":\n" + bodyStr + "===============\n" + resStr;
//                });

                return res;
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
