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

import java.util.concurrent.atomic.LongAdder;

import scala.collection.Seq;

import orc.ast.hasOptionalVariableName;
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
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
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
import com.oracle.truffle.api.utilities.AssumedValue;

/**
 *
 *
 * @author amp
 */
public abstract class InlinedCallRoot extends NodeBase {

    private final PorcERootNode targetRootNode;
    private final PorcEExecution execution;

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

    @Specialization(guards = {
            "body != null", "argumentSlots != null", "closureSlots != null"
            })
    public void impl(final VirtualFrame frame,
            final Object[] _arguments,
            @Cached("getPorcEBodyFrameArguments(frame)") Expression body,
            @Cached(value = "getArgumentSlots(frame)", dimensions = 1) FrameSlot[] argumentSlots,
            @Cached(value = "getClosureSlots(frame)", dimensions = 1) FrameSlot[] closureSlots,
            @Cached("createCountingProfile()") ConditionProfile inlinedTailCallProfile) {
        ensureTail(body);
        Object[] arguments = _arguments;
        // CompilerDirectives.ensureVirtualized(arguments);
        while(true) {
            long startTime = getProfilingScope().getTime();
            try {
                copyArgumentsToFrame(frame, arguments, argumentSlots, closureSlots);

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
                // Set the new arguments and fall through to go through the loop again.
                arguments = e.arguments;
            } finally {
                getProfilingScope().addTime(startTime);
            }
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
    private void copyArgumentsToFrame(final VirtualFrame frame, final Object[] arguments, FrameSlot[] argumentSlots,
            FrameSlot[] closureSlots) {
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

    protected FrameSlot[] getArgumentSlots(VirtualFrame frame) {
        CompilerAsserts.neverPartOfCompilation("Frame update");
        Seq<Variable> argumentVariables = targetRootNode.getArgumentVariables();
        FrameDescriptor descriptor = getCachedRootNode().getFrameDescriptor();
        FrameSlot[] res = new FrameSlot[argumentVariables.size()];

        for (int i = 0; i < res.length; i++) {
            Variable x = argumentVariables.apply(i);
            res[i] = descriptor.findOrAddFrameSlot(x, FrameSlotKind.Object);
        }
        return res;
    }

    protected FrameSlot[] getClosureSlots(VirtualFrame frame) {
        CompilerAsserts.neverPartOfCompilation("Frame update");
        Seq<Variable> closureVariables = targetRootNode.getClosureVariables();
        FrameDescriptor descriptor = getCachedRootNode().getFrameDescriptor();
        FrameSlot[] res = new FrameSlot[closureVariables.size()];

        for (int i = 0; i < res.length; i++) {
            Variable x = closureVariables.apply(i);
            res[i] = descriptor.findOrAddFrameSlot(x, FrameSlotKind.Object);
        }
        return res;
    }

    protected Expression getPorcEBodyFrameArguments(VirtualFrame frame) {
        CompilerAsserts.neverPartOfCompilation("Reconversion of code.");
//        Logger.info(() ->
//            String.format("%s@%08x: AST inlining %s@%08x (at %s)",
//                    getRootNode(), System.identityHashCode(getRootNode()),
//                    targetRootNode, System.identityHashCode(targetRootNode),
//                    this
//                    ));
        Expression body = targetRootNode.getBody();

        assert body.porcNode().isDefined();
        assert targetRootNode.getArgumentVariables() != null;

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

        return res;
    }

    public static boolean isAbove(final Node node, final RootNode targetRootNode) {
        if (node instanceof InlinedCallRoot) {
            if (((InlinedCallRoot)node).targetRootNode == targetRootNode) {
                return true;
            }
        }
        if (node.getParent() != null) {
            return isAbove(node.getParent(), targetRootNode);
        }

        if (!(node instanceof RootNode)) {
            Logger.info(() -> String.format("%s no parent",
                    node));
        }
        return false;
    }

    static InlinedCallRoot create(final RootNode targetRootNode, final PorcEExecution execution) {
        return InlinedCallRootNodeGen.create((PorcERootNode)targetRootNode, execution);
    }
}
