//
// InvokerCanInvoke.java -- Truffle node InvokerCanInvoke
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
import orc.run.porce.NodeBase;
import orc.values.sites.InvocableInvoker;
import orc.values.sites.OverloadedDirectInvokerBase1;
import orc.values.sites.OverloadedDirectInvokerBase2;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
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
@Introspectable
abstract class InvokerCanInvoke extends NodeBase {
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
    public abstract boolean executeCanInvoke(VirtualFrame frame, Invoker invoker, Object target, Object[] arguments);

    @Specialization(guards = { "isPartiallyEvaluable(invoker)" })
    public boolean partiallyEvaluable(Invoker invoker, Object target, Object[] arguments) {
        return invoker.canInvoke(target, arguments);
    }

    protected static boolean isPartiallyEvaluable(Invoker invoker) {
        return invoker instanceof InvocableInvoker ||
                invoker instanceof OverloadedDirectInvokerBase1 ||
                invoker instanceof OverloadedDirectInvokerBase2 ||
                invoker instanceof orc.compile.orctimizer.OrcAnnotation.Invoker ||
                invoker instanceof orc.values.sites.JavaArrayDerefSite.Invoker ||
                invoker instanceof orc.values.sites.JavaArrayAssignSite.Invoker ||
                invoker instanceof orc.run.extensions.SiteInvoker ||
                invoker instanceof orc.values.sites.JavaArrayLengthPseudofield.Invoker ||
                false;
    }

    @Specialization
    @TruffleBoundary(allowInlining = true)
    public boolean unknown(Invoker invoker, Object target, Object[] arguments) {
        return invoker.canInvoke(target, arguments);
    }

    public static InvokerCanInvoke create() {
        return InvokerCanInvokeNodeGen.create();
    }

}