//
// CallContinuation.java -- Java class CallContinuation
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.call;

import orc.run.porce.Expression;
import orc.run.porce.PorcEUnit;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecution;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

public class CallContinuation<ExternalDispatch extends Dispatch> extends Expression {
    @Child
    protected InternalCPSDispatch internalCall = null;

    @Child
    private Expression target;
    @Children
    private final Expression[] arguments;

    private final PorcEExecution execution;

    protected CallContinuation(final Expression target, final Expression[] arguments, final PorcEExecution execution) {
        this.target = target;
        this.arguments = arguments;
        this.execution = execution;
    }

    @Override
    public void setTail(boolean v) {
        super.setTail(v);
        if (internalCall != null) {
            internalCall.setTail(v);
        }
    }

    protected InternalCPSDispatch makeInternalCall() {
        return InternalCPSDispatch.createBare(false, execution);
    }

    protected InternalCPSDispatch getInternalCall() {
        if (internalCall == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            computeAtomicallyIfNull(() -> internalCall, (v) -> this.internalCall = v, () -> {
                InternalCPSDispatch n = insert(makeInternalCall());
                n.setTail(isTail);
                notifyInserted(n);
                return n;
            });
        }
        return internalCall;
    }

    @Override
    public Object execute(final VirtualFrame frame) {
        final Object targetValue = executeTargetObject(frame);
        final Object[] argumentValuesI = new Object[arguments.length + 1];
        executeArguments(frame, argumentValuesI, 1);
        argumentValuesI[0] = ((PorcEClosure) targetValue).environment;

        getInternalCall().executeDispatchWithEnvironment(frame, targetValue, argumentValuesI);
        return PorcEUnit.SINGLETON;
    }

    public static class CPS {
        public static Expression create(final Expression target, final Expression[] arguments,
                final PorcEExecution execution, boolean isTail) {
            if (isTail) {
                return createTail(target, arguments, execution);
            } else {
                return createNontail(target, arguments, execution);
            }
        }

        public static Expression createNontail(final Expression target, final Expression[] arguments,
                final PorcEExecution execution) {
            return CatchTailCall.create(createTail(target, arguments, execution), execution);
        }

        public static Expression createTail(final Expression target, final Expression[] arguments,
                final PorcEExecution execution) {
            return new CallContinuation<Dispatch>(target, arguments, execution);
        }
    }

    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.FULL_UNROLL)
    public void executeArguments(final VirtualFrame frame, final Object[] argumentValues, int offset) {
        assert argumentValues.length - offset == arguments.length;
        for (int i = 0; i < arguments.length; i++) {
            argumentValues[i + offset] = arguments[i].execute(frame);
        }
    }

    public Object executeTargetObject(final VirtualFrame frame) {
        return target.execute(frame);
    }
}
