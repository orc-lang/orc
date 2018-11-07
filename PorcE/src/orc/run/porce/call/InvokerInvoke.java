//
// InvokerInvokeDirect.java -- Truffle node InvokerInvokeDirect
// Project PorcE
//
// Created by amp on Jun 25, 2018.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.call;

import orc.Invoker;
import orc.SiteResponseSet;
import orc.VirtualCallContext;
import orc.run.porce.NodeBase;
import orc.run.porce.SpecializationConfiguration;
import orc.run.porce.runtime.PorcEExecution;
import orc.values.sites.InlinableInvoker;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Introspectable;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

/**
 * A node to call canInvoke on invokers.
 *
 * This is a separate node so that it can specialize on Invoker classes which can be optimized by Truffle.
 *
 * @author amp
 */
@ImportStatic({ SpecializationConfiguration.class })
@Introspectable
abstract class InvokerInvoke extends NodeBase {
    protected final PorcEExecution execution;

    protected InvokerInvoke(PorcEExecution execution) {
        this.execution = execution;
    }

    /**
     * Dispatch the call to the target with the given arguments.
     *
     * @param frame
     *            The frame we are executing in.
     * @param invoker
     *            The call invoker.
     * @param target
     *            The call target.
     * @param arguments
     *            The arguments to the call as expected by the invoker (without PCT).
     */
    public abstract SiteResponseSet executeInvoke(VirtualFrame frame, Invoker invoker, VirtualCallContext callContext, Object target, Object[] arguments);

    @Specialization(guards = { "KnownSiteSpecialization" })
    public SiteResponseSet partiallyEvaluable(InlinableInvoker invoker, VirtualCallContext callContext, Object target, Object[] arguments) {
        return invoker.invoke(callContext, target, arguments);
    }

    @Specialization
    @TruffleBoundary(allowInlining = true)
    public SiteResponseSet unknown(Invoker invoker, VirtualCallContext callContext, Object target, Object[] arguments) {
        return invoker.invoke(callContext, target, arguments);
    }


    public static InvokerInvoke create(PorcEExecution execution) {
        return InvokerInvokeNodeGen.create(execution);
    }
}
