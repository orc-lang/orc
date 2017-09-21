
package orc.run.porce.call;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;

import orc.CaughtEvent;
import orc.DirectInvoker;
import orc.Invoker;
import orc.error.runtime.ExceptionHaltException;
import orc.error.runtime.HaltException;
import orc.run.porce.SpecializationConfiguration;
import orc.run.porce.runtime.PorcEExecutionRef;
import orc.run.porce.runtime.PorcERuntime;

@ImportStatic({ SpecializationConfiguration.class })
public abstract class ExternalDirectDispatch extends DirectDispatch {
	protected ExternalDirectDispatch(final PorcEExecutionRef execution) {
		super(execution);
	}

	@CompilerDirectives.CompilationFinal(dimensions = 1)
	protected final BranchProfile[] exceptionProfiles = new BranchProfile[] 
			{ BranchProfile.create(), BranchProfile.create(), BranchProfile.create() };

	@Specialization(guards = { "canInvokeWithBoundary(invoker, target, arguments)" }, 
			limit = "ExternalDirectCallMaxCacheSize")
	public Object specific(final VirtualFrame frame, final Object target, final Object[] arguments,
			@Cached("getInvokerWithBoundary(target, arguments)") DirectInvoker invoker) {
		// DUPLICATION: This code is duplicated (mostly) in ExternalCPSDispatch.specificDirect.
		try {
			return invokeDirectWithBoundary(invoker, target, arguments);
		} catch (final ExceptionHaltException e) {
			exceptionProfiles[0].enter();
			execution.get().notifyOrcWithBoundary(new CaughtEvent(e.getCause()));
			throw HaltException.SINGLETON();
		} catch (final HaltException e) {
			exceptionProfiles[1].enter();
			throw e;
		} catch (final Exception e) {
			exceptionProfiles[2].enter();
			execution.get().notifyOrcWithBoundary(new CaughtEvent(e));
			throw HaltException.SINGLETON();
		}
	}

	@Specialization(replaces = { "specific" })
	public Object universal(final VirtualFrame frame, final Object target, final Object[] arguments) {
		try {
			final DirectInvoker invoker = getInvokerWithBoundary(target, arguments);
			return invokeDirectWithBoundary(invoker, target, arguments);
		} catch (final ExceptionHaltException e) {
			exceptionProfiles[0].enter();
			execution.get().notifyOrcWithBoundary(new CaughtEvent(e.getCause()));
			throw HaltException.SINGLETON();
		} catch (final HaltException e) {
			exceptionProfiles[1].enter();
			throw e;
		} catch (final Exception e) {
			exceptionProfiles[2].enter();
			execution.get().notifyOrcWithBoundary(new CaughtEvent(e));
			throw HaltException.SINGLETON();
		}
	}

	static ExternalDirectDispatch createBare(final PorcEExecutionRef execution) {
		return ExternalDirectDispatchNodeGen.create(execution);
	}

	/* Utilties */

	protected DirectInvoker getInvokerWithBoundary(final Object target, final Object[] arguments) {
		return (DirectInvoker) getInvokerWithBoundary(execution.get().runtime(), target, arguments);
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

	@TruffleBoundary(allowInlining = true, throwsControlFlowException = true)
	protected static Object invokeDirectWithBoundary(final DirectInvoker invoker, final Object target,
			final Object[] arguments) {
		return invoker.invokeDirect(target, arguments);
	}
}
