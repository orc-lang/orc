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

import orc.CallContext;
import orc.DirectInvoker;
import orc.Invoker;
import orc.error.runtime.HaltException;
import orc.error.runtime.JavaException;
import orc.run.extensions.SiteInvoker;
import orc.run.porce.NodeBase;
import orc.run.porce.SpecializationConfiguration;
import orc.run.porce.StackCheckingDispatch;
import orc.run.porce.runtime.CPSCallContext;
import orc.run.porce.runtime.PorcEExecution;
import orc.values.sites.InvocableInvoker;
import orc.values.sites.OverloadedDirectInvokerBase1;
import orc.values.sites.OverloadedDirectInvokerBase2;
import orc.values.sites.OrcJavaCompatibility;
import orc.values.Signal$;

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
    public abstract void executeInvoke(VirtualFrame frame, Invoker invoker, CallContext callContext, Object target, Object[] arguments);

    @Specialization(guards = { "isChannelGet(invoker)", "KnownSiteSpecialization" })
    public void channelGet(VirtualFrame frame, SiteInvoker invoker, CPSCallContext callContext, Object target, Object[] arguments,
            @Cached("create(execution)") StackCheckingDispatch dispatch) {
        orc.lib.state.Channel.ChannelInstance.GetSite getSite = (orc.lib.state.Channel.ChannelInstance.GetSite)target;
        Object v = performChannelGet(callContext, getSite);
        if (v != this) {
            dispatch.dispatch(frame, callContext.p(), v);
        }
    }

    @TruffleBoundary(allowInlining = true)
    private Object performChannelGet(CPSCallContext callContext,
            orc.lib.state.Channel.ChannelInstance.GetSite getSite) {
        Object v = this;
        synchronized (getSite.channel) {
            if (getSite.channel.contents.isEmpty()) {
                if (getSite.channel.closed) {
                    callContext.halt();
                } else {
                    callContext.setQuiescent();
                    getSite.channel.readers.addLast(callContext);
                }
            } else {
                // If there is an item available, pop it and return
                // it.
                Object v1 = orc.values.sites.compatibility.SiteAdaptor.object2value(getSite.channel.contents.removeFirst());
                if (getSite.channel.closer != null && getSite.channel.contents.isEmpty()) {
                    getSite.channel.closer.publish(Signal$.MODULE$);
                    getSite.channel.closer = null;
                }
                if (callContext.publishOptimized()) {
                    v = v1;
                }
            }
        }
        return v;
    }

    protected static boolean isChannelGet(SiteInvoker invoker) {
        return invoker.siteCls() == orc.lib.state.Channel.ChannelInstance.GetSite.class;
    }

    @Specialization
    @TruffleBoundary(allowInlining = true, transferToInterpreterOnException = false)
    public void unknown(Invoker invoker, CallContext callContext, Object target, Object[] arguments) {
        invoker.invoke(callContext, target, arguments);
    }


    public static InvokerInvoke create(PorcEExecution execution) {
        return InvokerInvokeNodeGen.create(execution);
    }
}
