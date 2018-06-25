//
// InterceptedDispatch.java -- Java class InterceptedDispatch
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.call;

import orc.run.porce.runtime.CPSCallContext;
import orc.run.porce.runtime.Counter;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecution;
import orc.run.porce.runtime.Terminator;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Instrumentable;

@Instrumentable(factory = InterceptedDispatchWrapper.class)
public abstract class InterceptedDispatch extends Dispatch {
    protected InterceptedDispatch(InterceptedDispatch orig) {
        this(orig.execution);
    }

    protected InterceptedDispatch(PorcEExecution execution) {
        super(execution);
    }

    @Specialization
    public void run(final VirtualFrame frame, final Object newTarget, final Object[] newArguments) {
        final PorcEClosure pub = (PorcEClosure) newArguments[0];
        final Counter counter = (Counter) newArguments[1];
        final Terminator term = (Terminator) newArguments[2];

        // Token: Passed to callContext from arguments.
        final CPSCallContext callContext = new CPSCallContext(execution, pub, counter, term, getCallSiteId());

        invokeInterceptedWithBoundary(callContext, newTarget, newArguments);
    }

    @TruffleBoundary
    private void invokeInterceptedWithBoundary(final CPSCallContext callContext, final Object newTarget,
            final Object[] newArguments) {
        execution.invokeIntercepted(callContext, newTarget, buildArgumentValues(newArguments));
    }

    protected Object[] buildArgumentValues(final Object[] newArguments) {
        final Object[] argumentValues = new Object[newArguments.length - 3];
        System.arraycopy(newArguments, 3, argumentValues, 0, argumentValues.length);
        return argumentValues;
    }

    static InterceptedDispatch create(PorcEExecution execution) {
        return InterceptedDispatchNodeGen.create(execution);
    }
}
