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
import orc.run.porce.runtime.CallClosureSchedulable;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecution;
import orc.run.porce.runtime.PorcERuntime;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Instrumentable;
import com.oracle.truffle.api.profiles.ConditionProfile;

/**
 *
 *
 * @author amp
 */
@AdhocIntrospectable
@Instrumentable(factory = StackCheckingDispatchWrapper.class)
public class StackCheckingDispatch extends Dispatch {
    @Child
    protected Dispatch call = null;

    public final ConditionProfile spawnProfile = ConditionProfile.createCountingProfile();

    private StackCheckingDispatch(PorcEExecution execution) {
	super(execution);
    }

    StackCheckingDispatch(StackCheckingDispatch orig) {
	super(orig.execution);
    }

    @Override
    public void executeDispatch(VirtualFrame frame, Object target, Object[] arguments) {
	//CompilerAsserts.compilationConstant(arguments.length);
        final PorcEClosure computation = (PorcEClosure) target;
	Object[] newArguments = new Object[arguments.length + 1];
	newArguments[0] = computation.environment;
	System.arraycopy(arguments, 0, newArguments, 1, arguments.length);
	executeDispatchWithEnvironment(frame, target, arguments);
    }

    @Override
    public void executeDispatchWithEnvironment(VirtualFrame frame, Object target, Object[] args) {
	final PorcERuntime r = execution.runtime();
        final PorcEClosure computation = (PorcEClosure) target;
        PorcERuntime.StackDepthState state = r.incrementAndCheckStackDepth();
        final int prev = state.previousDepth();
	if (spawnProfile.profile(state.growthAllowed())) {
            executeInline(frame, computation, args, prev);
        } else {
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
		Dispatch n = insert(InternalCPSDispatch.create(false, execution, isTail));
		n.setTail(isTail);
		notifyInserted(n);
		return n;
	    });
	}
	return call;
    }

    private void executeInline(final VirtualFrame frame, final PorcEClosure computation, final Object[] args, final int previous) {
	final PorcERuntime r = execution.runtime();
	Object old = SimpleWorkStealingSchedulerWrapper.currentSchedulable();
	long id = SimpleWorkStealingSchedulerWrapper.enterSchedulableInline();
	try {
	    getCall().executeDispatchWithEnvironment(frame, computation, args);
	} finally {
	    if (previous >= 0) {
                r.decrementStackDepth(previous);
            }
	    SimpleWorkStealingSchedulerWrapper.exitSchedulable(id, old);
	}
    }

    public void executeInline(final VirtualFrame frame, final PorcEClosure computation, final int previous) {
	executeInline(frame, computation, new Object[] { null }, previous);
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
        properties.put("spawnProfile", spawnProfile);
        return properties;
    }

    public static StackCheckingDispatch create(PorcEExecution execution) {
	return new StackCheckingDispatch(execution);
    }
}
