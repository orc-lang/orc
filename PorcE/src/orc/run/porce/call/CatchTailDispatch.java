//
// CatchTailDispatch.java -- Java class CatchTailDispatch
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.call;

import orc.run.porce.runtime.PorcEExecution;
import orc.run.porce.runtime.TailCallException;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;

public class CatchTailDispatch extends Dispatch {
    @Child
    protected TailCallLoop loop;
    @Child
    protected Dispatch body;

    @Override
    public void setTail(boolean v) {
        super.setTail(v);
        body.setTail(v);
    }

    protected CatchTailDispatch(final Dispatch body, final PorcEExecution execution) {
        super(execution);
        this.body = body;
        this.loop = TailCallLoop.create(execution);
    }

    @SuppressWarnings("boxing")
    @Override
    public void executeDispatch(VirtualFrame frame, Object target, Object[] arguments) {
        CompilerAsserts.compilationConstant(isTail);
        if (isTail) {
            body.executeDispatch(frame, target, arguments);
        } else {
            try {
                body.executeDispatch(frame, target, arguments);
            } catch (TailCallException e) {
                loop.executeTailCalls(frame, e);
            }
        }
    }

    @SuppressWarnings("boxing")
    @Override
    public void executeDispatchWithEnvironment(VirtualFrame frame, Object target, Object[] arguments) {
        CompilerAsserts.compilationConstant(isTail);
        if (isTail) {
            body.executeDispatchWithEnvironment(frame, target, arguments);
        } else {
            try {
                body.executeDispatchWithEnvironment(frame, target, arguments);
            } catch (TailCallException e) {
                loop.executeTailCalls(frame, e);
            }
        }
    }

    public static CatchTailDispatch create(final Dispatch body, final PorcEExecution execution) {
        return new CatchTailDispatch(body, execution);
    }
}
