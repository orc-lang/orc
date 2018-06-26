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

import orc.DirectInvoker;
import orc.Invoker;
import orc.error.runtime.HaltException;
import orc.error.runtime.JavaException;
import orc.run.porce.NodeBase;
import orc.run.porce.SpecializationConfiguration;
import orc.run.porce.runtime.CPSCallContext;
import orc.values.sites.InvocableInvoker;
import orc.values.sites.OverloadedDirectInvokerBase1;
import orc.values.sites.OverloadedDirectInvokerBase2;
import orc.values.sites.OrcJavaCompatibility;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.InvocationTargetException;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
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
    public abstract void executeInvoke(VirtualFrame frame, Invoker invoker, CPSCallContext callContext, Object target, Object[] arguments);

    @Specialization(guards = { "ExternalCPSDirectSpecialization" })
    public void unknownDirect(VirtualFrame frame, DirectInvoker invoker, CPSCallContext callContext, Object target, Object[] arguments,
            @Cached("create()") InvokerInvokeDirect invokeDirect) {
        try {
            callContext.publish(invokeDirect.executeInvokeDirect(frame, invoker, target, arguments));
        } catch (HaltException e) {
            callContext.halt();
        }
    }

    @Specialization
    @TruffleBoundary(allowInlining = true, transferToInterpreterOnException = false)
    public void unknown(Invoker invoker, CPSCallContext callContext, Object target, Object[] arguments) {
        invoker.invoke(callContext, target, arguments);
    }

    public static InvokerInvoke create() {
        return InvokerInvokeNodeGen.create();
    }
}