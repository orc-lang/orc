//
// StackCheckingExpression.java -- Abstract truffle node StackCheckingExpression
// Project PorcE
//
// Created by amp on Mar 28, 2018.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce;

import java.util.Map;

import orc.run.porce.call.Dispatch;
import orc.run.porce.call.InternalCPSDispatch;
import orc.run.porce.instruments.AdhocIntrospectable;
import orc.run.porce.profiles.VisibleConditionProfile;
import orc.run.porce.runtime.CallClosureSchedulable;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecution;
import orc.run.porce.runtime.PorcERuntime;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Instrumentable;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.profiles.ConditionProfile;

/**
 *
 *
 * @author amp
 */
@AdhocIntrospectable
@Instrumentable(factory = StackCheckingDispatchWrapper.class)
public class StackCheckingDispatch extends Dispatch implements HasCalledRoots {
    @Child
    protected Dispatch call = null;

    private final VisibleConditionProfile inlineProfile = VisibleConditionProfile.createBinaryProfile();
    private final ConditionProfile unrollProfile = ConditionProfile.createBinaryProfile();

    @Override
    public boolean isScheduled() {
        return true;
    }

    private StackCheckingDispatch(PorcEExecution execution) {
        super(execution);
    }

    StackCheckingDispatch(StackCheckingDispatch orig) {
        super(orig.execution);
    }

    @Override
    public void executeDispatch(VirtualFrame frame, Object target, Object[] arguments) {
        // CompilerAsserts.compilationConstant(arguments.length);
        final PorcEClosure computation = (PorcEClosure) target;
        Object[] newArguments = new Object[arguments.length + 1];
        newArguments[0] = computation.environment;
        System.arraycopy(arguments, 0, newArguments, 1, arguments.length);
        executeDispatchWithEnvironment(frame, target, arguments);
    }

    @Override
    public void executeDispatchWithEnvironment(VirtualFrame frame, Object target, Object[] args) {
        final PorcEClosure computation = (PorcEClosure) target;

        // If this node has never actually had to spawn and there is an enclosing
        // StackCheckingDispatch in this function then don't even count this frame.
//        if (!inlineProfile.wasFalse() && NodeUtil.findParent(this, StackCheckingDispatch.class) != null) {
//            executeInline(frame, computation, args);
//            return;
//        }

        final PorcERuntime r = execution.runtime();
        PorcERuntime.StackDepthState state = r.incrementAndCheckStackDepth(inlineProfile);
        final int prev = state.previousDepth();
        if (inlineProfile.profile(state.growthAllowed())) {
            executeInline(frame, computation, args, prev);
        } else {
            addCalledRoot(computation.body);
            createSchedulableAndSchedule(args, computation);
        }
    }

    /**
     * @param args
     * @param computation
     */
    @TruffleBoundary(allowInlining = false)
    private void createSchedulableAndSchedule(final Object[] args, final PorcEClosure computation) {
        execution.runtime().schedule(CallClosureSchedulable.varArgs(computation, args, execution));
    }

    protected Dispatch getCall() {
        if (call == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            computeAtomicallyIfNull(() -> call, (v) -> call = v, () -> {
                Dispatch n = insert(InternalCPSDispatch.create(execution, isTail));
                n.setTail(isTail);
                notifyInserted(n);
                return n;
            });
        }
        return call;
    }

    private void executeInline(final VirtualFrame frame, final PorcEClosure computation, final Object[] args,
            final int previous) {
        final PorcERuntime r = execution.runtime();
        Object old = SimpleWorkStealingSchedulerWrapper.currentSchedulable();
        long id = SimpleWorkStealingSchedulerWrapper.enterSchedulableInline();
        try {
            getCall().executeDispatchWithEnvironment(frame, computation, args);
        } finally {
            r.decrementStackDepth(previous, unrollProfile);
            SimpleWorkStealingSchedulerWrapper.exitSchedulable(id, old);
        }
    }

    private void executeInline(final VirtualFrame frame, final PorcEClosure computation, final Object[] args) {
        Object old = SimpleWorkStealingSchedulerWrapper.currentSchedulable();
        long id = SimpleWorkStealingSchedulerWrapper.enterSchedulableInline();
        try {
            getCall().executeDispatchWithEnvironment(frame, computation, args);
        } finally {
            SimpleWorkStealingSchedulerWrapper.exitSchedulable(id, old);
        }
    }

    public void executeInline(final VirtualFrame frame, final PorcEClosure computation) {
        executeInline(frame, computation, new Object[] { null });
    }

    @Override
    public void setTail(boolean b) {
        super.setTail(b);
        if (call != null) {
            call.setTail(b);
        }
    }

    @Override
    public Map<String, Object> getDebugProperties() {
        Map<String, Object> properties = super.getDebugProperties();
        properties.put("inlineProfile", inlineProfile);
        return properties;
    }

    public static StackCheckingDispatch create(PorcEExecution execution) {
        return new StackCheckingDispatch(execution);
    }
}
