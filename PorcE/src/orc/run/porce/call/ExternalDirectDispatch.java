//
// ExternalDirectDispatch.java -- Java class ExternalDirectDispatch
// Project PorcE
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
import orc.run.porce.SpecializationConfiguration;
import orc.run.porce.profiles.ValueClassesProfile;
import orc.run.porce.runtime.PorcEExecution;
import orc.run.porce.runtime.PorcERuntime;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Introspectable;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Instrumentable;
import com.oracle.truffle.api.profiles.BranchProfile;

@ImportStatic({ SpecializationConfiguration.class })
@Introspectable
@Instrumentable(factory = ExternalDirectDispatchWrapper.class)
public abstract class ExternalDirectDispatch extends DirectDispatch {
    protected ExternalDirectDispatch(final PorcEExecution execution) {
        super(execution);
    }

    protected ExternalDirectDispatch(final ExternalDirectDispatch orig) {
        super(orig.execution);
    }

    protected final BranchProfile exceptionProfile = BranchProfile.create();
    protected final ValueClassesProfile argumentClassesProfile = new ValueClassesProfile();

    @Specialization(guards = {
            "canInvoke.executeCanInvoke(frame, invoker, target, argumentClassesProfile.profile(arguments))" },
        limit = "ExternalDirectCallMaxCacheSize")
    public Object specific(final VirtualFrame frame, final Object target, final Object[] arguments,
            @Cached("getInvokerWithBoundary(target, arguments)") DirectInvoker invoker,
            @Cached("create()") InvokerCanInvoke canInvoke,
            @Cached("create(execution)") InvokerInvokeDirect invokeDirect) {
        // DUPLICATION: This code is duplicated (mostly) in ExternalCPSDispatch.specificDirect.
        try {
            return invokeDirect.executeInvokeDirect(frame, invoker, target, argumentClassesProfile.profile(arguments));
        } catch (final HaltException e) {
            throw e;
        } catch (final Throwable e) {
            exceptionProfile.enter();
            execution.notifyOfException(e, this);
            throw HaltException.SINGLETON();
        }
    }

    @Specialization() // replaces = { "specific" })
    public Object universal(final VirtualFrame frame, final Object target, final Object[] arguments,
        @Cached("create(execution)") InvokerInvokeDirect invokeDirect) {
        // FIXME: This is much better for code de-duplication however if getInvokerWithBoundary throws an exception then
        // this will break.
        final DirectInvoker invoker = getInvokerWithBoundary(target, argumentClassesProfile.profile(arguments));
        return specific(frame, target, arguments, invoker, null, invokeDirect);
    }

    static ExternalDirectDispatch createBare(final PorcEExecution execution) {
        return ExternalDirectDispatchNodeGen.create(execution);
    }

    /* Utilties */

    protected DirectInvoker getInvokerWithBoundary(final Object target, final Object[] arguments) {
        return (DirectInvoker) getInvokerWithBoundary(execution.runtime(), target, arguments);
    }

    @TruffleBoundary(allowInlining = true)
    protected static Invoker getInvokerWithBoundary(final PorcERuntime runtime, final Object target,
            final Object[] arguments) {
        return runtime.getInvoker(target, arguments);
    }
}
