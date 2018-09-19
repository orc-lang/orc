//
// TailCallLoop.java -- Java class TailCallLoop
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.call;

import java.util.concurrent.locks.Lock;

import orc.run.porce.NodeBase;
import orc.run.porce.PorcERootNode;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecution;
import orc.run.porce.runtime.TailCallException;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RepeatingNode;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;

public class TailCallLoop extends NodeBase {
    @Child
    protected volatile LoopNode loop = null;

    protected final PorcEExecution execution;

    protected TailCallLoop(final PorcEExecution execution) {
        this.execution = execution;
    }

    private LoopNode getLoopNode(VirtualFrame frame) {
        if (loop == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            computeAtomicallyIfNull(() -> loop, (v) -> loop = v, () -> {
                LoopNode n = insert(Truffle.getRuntime()
                        .createLoopNode(new CatchTailCallRepeatingNode(frame.getFrameDescriptor(), execution)));
                notifyInserted(n);
                return n;
            });
        }
        return loop;
    }

    public void executeTailCalls(VirtualFrame frame, TailCallException e) {
        LoopNode loop = getLoopNode(frame);
        CatchTailCallRepeatingNode repeating = (CatchTailCallRepeatingNode) loop.getRepeatingNode();
        repeating.initFrame(frame);
        repeating.setNextCall(frame, e.target, e.arguments);
        loop.executeLoop(frame);
    }

    public static TailCallLoop create(final PorcEExecution execution) {
        return new TailCallLoop(execution);
    }

    protected abstract static class TailCallNode extends NodeBase {
        protected final PorcEExecution execution;

        protected TailCallNode(PorcEExecution execution) {
            this.execution = execution;
        }
    }

    protected final static class TailCallSpecializationNode extends TailCallNode {
        @Child
        protected DirectCallNode call;
        @Child
        protected TailCallNode next;

        protected final RootCallTarget target;
        protected final ConditionProfile thisOrOtherProfile = ConditionProfile.createCountingProfile();

        protected TailCallSpecializationNode(PorcEClosure target, TailCallNode next, PorcEExecution execution) {
            super(execution);
            this.target = target.body;
            this.call = DirectCallNode.create(target.body);
            this.next = next;
        }

        protected boolean matchesSpecific(PorcEClosure target) {
            return this.target == target.body;
        }

        public static TailCallNode create(final PorcEClosure target, final TailCallNode next,
                final PorcEExecution execution) {
            return new TailCallSpecializationNode(target, next, execution);
        }
    }

    protected final static class TailCallTerminalNode extends TailCallNode {
        protected TailCallTerminalNode(PorcEExecution execution) {
            super(execution);
        }

        public static TailCallNode create(final PorcEExecution execution) {
            return new TailCallTerminalNode(execution);
        }
    }

    protected static final class CatchTailCallRepeatingNode extends Node implements RepeatingNode {
        private final PorcEExecution execution;

        private final BranchProfile tailCallProfile = BranchProfile.create();
        private final BranchProfile returnProfile = BranchProfile.create();

        private final FrameSlot targetSlot;
        private final FrameSlot argumentsSlot;
        private final FrameSlot tceSlot;

        @Child
        protected TailCallNode call;

        public CatchTailCallRepeatingNode(FrameDescriptor frameDescriptor, PorcEExecution execution) {
            this.execution = execution;
            this.tceSlot = frameDescriptor.findOrAddFrameSlot("<tailCallException>", FrameSlotKind.Object);
            this.targetSlot = frameDescriptor.findOrAddFrameSlot("<OSRtailCallTarget>", FrameSlotKind.Object);
            this.argumentsSlot = frameDescriptor.findOrAddFrameSlot("<OSRtailCallArguments>", FrameSlotKind.Object);
            this.call = TailCallTerminalNode.create(execution);
        }

        public void initFrame(VirtualFrame frame) {
            frame.setObject(tceSlot, TailCallException.create(null));
        }

        public TailCallException getTCE(VirtualFrame frame) {
            TailCallException tce = (TailCallException) FrameUtil.getObjectSafe(frame, tceSlot);
            return tce;
        }

        public void setNextCall(VirtualFrame frame, PorcEClosure target, Object[] arguments) {
            frame.setObject(targetSlot, target);
            frame.setObject(argumentsSlot, arguments);
        }

        private Object getTarget(VirtualFrame frame) {
            Object target = FrameUtil.getObjectSafe(frame, targetSlot);
            frame.setObject(targetSlot, null); // Clear it to avoid leaking during the call
            return target;
        }

        private Object[] getArguments(VirtualFrame frame) {
            Object[] arguments = (Object[]) FrameUtil.getObjectSafe(frame, argumentsSlot);
            // TODO: Evaluate in depth if this is good or bad. It showed up on a profile and
            // probably will not help much because arguments will still be stored in the frame.
            frame.setObject(argumentsSlot, null); // Clear it to avoid leaking during the call
            return arguments;
        }

        @Override
        public boolean executeRepeating(VirtualFrame frame) {
            try {
                Object target = getTarget(frame);
                Object[] arguments = getArguments(frame);
                invokeCalls(call, frame, (PorcEClosure) target, arguments);
                returnProfile.enter();
                return false;
            } catch (TailCallException e) {
                tailCallProfile.enter();
                setNextCall(frame, e.target, e.arguments);
                return true;
            }
        }

        @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.FULL_UNROLL)
        private void invokeCalls(TailCallNode call, final VirtualFrame frame, PorcEClosure target, Object[] arguments) {
            boolean isFirstCall = true;
            // getRootNode().getCurrentContext(orc.run.porce.PorcELanguage.class);
            while (true) {
                if (call instanceof TailCallSpecializationNode) {
                    TailCallSpecializationNode spec = (TailCallSpecializationNode) call;
                    if ((spec.next instanceof TailCallTerminalNode && spec.matchesSpecific(target))
                            || spec.thisOrOtherProfile.profile(spec.matchesSpecific(target))) {
                        try {
                            isFirstCall = false;
                            spec.call.call(arguments);
                            break;
                        } catch (TailCallException e) {
                            target = e.target;
                            arguments = e.arguments;
                            call = spec.next;
                            continue;
                        }
                    } else {
                        call = spec.next;
                        continue;
                    }
                } else {
                    assert call instanceof TailCallTerminalNode;
                    if (isFirstCall) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        Lock lock = getLock();
                        lock.lock();
                        try {
                            TailCallTerminalNode term = (TailCallTerminalNode) call;
                            TailCallNode n = term.replace(TailCallSpecializationNode.create(target,
                                    TailCallTerminalNode.create(execution), execution), "Extend TailCall chain");
                            // final Object t = target;
                            // Logger.info(() -> "Extending " + getRootNode().getName() + " with " + t);
                            call = n;
                            continue;
                        } finally {
                            lock.unlock();
                        }
                    }
                    TailCallException tce = getTCE(frame);
                    tce.target = target;
                    tce.arguments = arguments;
                    throw tce;
                }
            }
        }

        @Override
        public String toString() {
            RootNode root = getRootNode();
            if (root instanceof PorcERootNode) {
                return ((PorcERootNode) root).toString() + "<tailrecloop>";
            } else {
                return "unknown<tailrecloop@" + hashCode() + ">";
            }
        }
    }

}
