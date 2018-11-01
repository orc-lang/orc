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
import orc.run.porce.SpecializationConfiguration;
import orc.values.sites.InvocableInvoker;
import orc.values.sites.InvocationBehaviorUtilities;
import orc.values.sites.OverloadedDirectInvokerBase1;
import orc.values.sites.OverloadedDirectInvokerBase2;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Introspectable;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

/**
 * A node to call canInvoke on invokers.
 *
 * This is a separate node so that it can specialize on Invoker classes which can be optimized by Truffle.
 *
 * @author amp
 */
@ImportStatic({ SpecializationConfiguration.class })
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

    @Specialization(guards = { "KnownSiteSpecialization", "invoker.argumentClss() == argumentClss" })
    public boolean invocableInvoker(InvocableInvoker invoker, Object target, Object[] arguments,
            @Cached(value = "invoker.argumentClss()", dimensions = 1) final Class<?>[] argumentClss) {
        CompilerAsserts.compilationConstant(invoker);
        return invoker.canInvokeTarget(target) &&
                valuesHaveType(arguments, argumentClss);
    }

    @Specialization(guards = { "KnownSiteSpecialization",
            "invoker.targetCls() == targetCls", "invoker.argumentClss() == argumentClss" })
    public boolean invocableInvoker(orc.values.sites.TargetClassAndArgumentClassSpecializedInvoker invoker,
            Object target, Object[] arguments,
            @Cached(value = "invoker.targetCls()") final Class<?> targetCls,
            @Cached(value = "invoker.argumentClss()", dimensions = 1) final Class<?>[] argumentClss) {
        CompilerAsserts.compilationConstant(invoker);
        return InvocationBehaviorUtilities.valueHasType(target, targetCls) &&
                valuesHaveType(arguments, argumentClss);
    }

    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.FULL_UNROLL)
    private final boolean valuesHaveType(Object[] arguments, Class<?>[] argumentClss) {
        if (arguments.length != argumentClss.length) {
          return false;
        }
        // Conceptually: (argumentClss zip arguments).forall(... Predicate ...)
        int i = 0;
        boolean res = true;
        while (i < argumentClss.length && res) {
            // Predicate here:
            res = res && InvocationBehaviorUtilities.valueHasType(arguments[i], argumentClss[i]);
            i += 1;
        }
        return res;
    }

    @Specialization(guards = { "isPartiallyEvaluable(invoker)" })
    public boolean partiallyEvaluable(Invoker invoker, Object target, Object[] arguments) {
        return invoker.canInvoke(target, arguments);
    }

    protected static boolean isPartiallyEvaluable(Invoker invoker) {
        return  invoker instanceof OverloadedDirectInvokerBase1 ||
                invoker instanceof OverloadedDirectInvokerBase2 ||
                invoker instanceof orc.values.sites.TargetValueAndArgumentClassSpecializedInvoker ||
                invoker instanceof orc.values.sites.TargetClassAndArgumentClassSpecializedInvoker ||
                invoker instanceof orc.compile.orctimizer.OrcAnnotation.Invoker ||
                invoker instanceof orc.values.sites.JavaArrayDerefSite.Invoker ||
                invoker instanceof orc.values.sites.JavaArrayAssignSite.Invoker ||
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