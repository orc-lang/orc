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

	@Specialization(guards = { "canInvokeWithBoundary(invoker, target, arguments)" }, 
			limit = "ExternalDirectCallMaxCacheSize")
	public Object specific(final VirtualFrame frame, final Object target, final Object[] arguments,
			@Cached("getInvokerWithBoundary(target, arguments)") DirectInvoker invoker) {
		// DUPLICATION: This code is duplicated (mostly) in ExternalCPSDispatch.specificDirect.
		try {
			return invokeDirectWithBoundary(invoker, target, arguments);
		}  catch (final Throwable e) {
            exceptionProfile.enter();
            execution.notifyOfException(e, this);
            throw HaltException.SINGLETON();
		}
	}

	@Specialization(replaces = { "specific" })
	public Object universal(final VirtualFrame frame, final Object target, final Object[] arguments) {
	    // FIXME: This is much better for code de-duplication however if getInvokerWithBoundary throws an exception then this will break.
		final DirectInvoker invoker = getInvokerWithBoundary(target, arguments);
		return specific(frame, target, arguments, invoker);
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

	@TruffleBoundary(allowInlining = true)
	protected static boolean canInvokeWithBoundary(final Invoker invoker, final Object target,
			final Object[] arguments) {
		return invoker.canInvoke(target, arguments);
	}

	@TruffleBoundary(allowInlining = true, transferToInterpreterOnException = false)
	protected static Object invokeDirectWithBoundary(final DirectInvoker invoker, final Object target,
			final Object[] arguments) {
		return invoker.invokeDirect(target, arguments);
	}
}
