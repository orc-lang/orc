//
// InlinedCallRoot.java -- Truffle node InlinedCallRoot
// Project PorcE
//
// Created by amp on Sep 27, 2018.
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
import orc.run.porce.Expression;
import orc.run.porce.Logger;
import orc.run.porce.NodeBase;
import orc.run.porce.PorcERootNode;
import orc.run.porce.ProfilingScope;
import orc.run.porce.runtime.InlinedTailCallException;
import orc.run.porce.runtime.PorcEExecution;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.graalvm.collections.Pair;

/**
 *
 *
 * @author amp
 */
public abstract class InlinedCallRoot extends NodeBase {

    protected final PorcERootNode targetRootNode;
    private final PorcEExecution execution;

    @Child
    protected Expression inlinedBody;
    @CompilationFinal(dimensions=1)
    private FrameSlot[] argumentSlots;
    @CompilationFinal(dimensions=1)
    private FrameSlot[] closureSlots;

    protected InlinedCallRoot(final PorcERootNode targetRootNode, final PorcEExecution execution) {
        this.targetRootNode = targetRootNode;
        this.execution = execution;
    }

    @Override
    public ProfilingScope getProfilingScope() {
        return targetRootNode.getProfilingScope();
    }

    abstract void execute(final VirtualFrame frame, Object[] arguments);

    @Override
    public String getContainingPorcCallableName() {
        return targetRootNode.getContainingPorcCallableName();
    }

    @Specialization()
    public void impl(final VirtualFrame frame,
            final Object[] arguments,
            @Cached("createCountingProfile()") ConditionProfile inlinedTailCallProfile) {
        Expression body = getPorcEBodyFrameArguments();
        ensureTail(body);
        CompilerDirectives.ensureVirtualized(arguments);
        copyArgumentsToFrame(frame, arguments);
        try {
            while(true) {
                long startTime = getProfilingScope().getTime();
                try {
                    body.execute(frame);

                    // No tail call so break out of the loop.
                    break;
                } catch (final InlinedTailCallException e) {
                    if (inlinedTailCallProfile.profile(e.target != targetRootNode)) {
                        //Logger.info(() ->
                        //    "Rethrowing InlinedTailCall to " + e.target + System.identityHashCode(e.target) +
                        //    " at inlined copy of " + targetRootNode + System.identityHashCode(e.target));
                        throw e;
                    }
                } finally {
                    getProfilingScope().addTime(startTime);
                }
            }
        } finally {
            clearFrame(frame);
        }
    }

    public boolean isApplicable(final VirtualFrame frame) {
        CompilerAsserts.neverPartOfCompilation("Slow isApplicable");

        Seq<Variable> argumentVariables = targetRootNode.getArgumentVariables();
        Seq<Variable> closureVariables = targetRootNode.getClosureVariables();

        if (closureVariables == null || argumentVariables == null) {
            return false;
        }
        if (!targetRootNode.getBody().porcNode().isDefined()) {
            Logger.warning(() -> this + " in root node w/o Porc node: " + targetRootNode.getBody());
            return false;
        }

        return true;
    }

    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.FULL_UNROLL)
    void copyArgumentsToFrame(final VirtualFrame frame, final Object[] arguments) {
        if (arguments.length != argumentSlots.length + 1) {
            CompilerDirectives.transferToInterpreter();
            throw new ArityMismatchException(arguments.length - 1, argumentSlots.length);
        }

        for (int i = 0; i < argumentSlots.length; i++) {
            FrameSlot slot = argumentSlots[i];
            frame.setObject(slot, arguments[i+1]);
        }

        Object[] environment = (Object[]) arguments[0];
        for (int i = 0; i < closureSlots.length; i++) {
            FrameSlot slot = closureSlots[i];
            frame.setObject(slot, environment[i]);
        }
    }
    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.FULL_UNROLL)
    void clearFrame(final VirtualFrame frame) {
        for (int i = 0; i < argumentSlots.length; i++) {
            FrameSlot slot = argumentSlots[i];
            frame.setObject(slot, null);
        }
        for (int i = 0; i < closureSlots.length; i++) {
            FrameSlot slot = closureSlots[i];
            frame.setObject(slot, null);
        }
    }

    protected FrameSlot[] getSlots(Seq<Variable> variables) {
        CompilerAsserts.neverPartOfCompilation("Frame update");
        FrameDescriptor descriptor = getCachedRootNode().getFrameDescriptor();
        FrameSlot[] res = new FrameSlot[variables.size()];

        for (int i = 0; i < res.length; i++) {
            Variable x = variables.apply(i);
            res[i] = descriptor.findOrAddFrameSlot(variableSubst(x), FrameSlotKind.Object);
        }
        return res;
    }

    protected Expression getPorcEBodyFrameArguments() {
        if (inlinedBody == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            computeAtomicallyIfNull(() -> inlinedBody, (v) -> inlinedBody = v, () -> {
//                Logger.log(Level.INFO, () ->
//                    String.format("%s@%08x: AST inlining %s@%08x (at %s)",
//                            getRootNode(), System.identityHashCode(getRootNode()),
//                            targetRootNode, System.identityHashCode(targetRootNode),
//                            this
//                            ));
                Expression body = targetRootNode.getBody();

                assert body.porcNode().isDefined();

                // Now reconvert the original Porc code to PorcE in a new context.
                Expression res = execution.porcToPorcE().expression(
                        (orc.ast.porc.Expression.Z)body.porcNode().get(),
                        PorcToPorcE.variableSeq(),
                        PorcToPorcE.variableSeq(),
                        getCachedRootNode().getFrameDescriptor(),
                        execution.callTargetMap(),
                        this::variableSubst,
                        isTail,
                        (orc.ast.porc.Variable)targetRootNode.getMethodKey()
                        );

                argumentSlots = getSlots(targetRootNode.getArgumentVariables());
                closureSlots = getSlots(targetRootNode.getClosureVariables());

                return insert(res);
            });
        }

        return inlinedBody;
    }

    protected Object variableSubst(orc.ast.porc.Variable v) {
        return Pair.create(this, v);
    }

    public static InlinedCallRoot findInlinedCallRoot(final Node node, final RootNode targetRootNode) {
        if (node instanceof InlinedCallRoot) {
            if (((InlinedCallRoot)node).targetRootNode == targetRootNode) {
                return (InlinedCallRoot)node;
            }
        }
        if (node.getParent() != null) {
            return findInlinedCallRoot(node.getParent(), targetRootNode);
        }

        if (!(node instanceof RootNode)) {
            Logger.info(() -> String.format("%s no parent", node));
        }
        return null;
    }

    public static boolean isInMethod(final Node node, final RootNode rootNode) {
        if (rootNode instanceof PorcERootNode) {
            return isInMethod(node, ((PorcERootNode)rootNode).getMethodKey());
        }
        return false;
    }

    public static boolean isInMethod(final Node node, final Object key) {
        if (node instanceof InlinedCallRoot && ((InlinedCallRoot)node).targetRootNode.getMethodKey() == key) {
            return true;
        }
        if (node instanceof PorcERootNode && ((PorcERootNode)node).getMethodKey() == key) {
            return true;
        }
        if (node.getParent() != null) {
            return isInMethod(node.getParent(), key);
        }
        if (!(node instanceof RootNode)) {
            Logger.info(() -> String.format("%s no parent", node));
        }
        return false;
    }

    static InlinedCallRoot create(final RootNode targetRootNode, final PorcEExecution execution) {
        return InlinedCallRootNodeGen.create((PorcERootNode)targetRootNode, execution);
    }
}
