
package orc.run.porce.call;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Introspectable;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Instrumentable;
import com.oracle.truffle.api.profiles.BranchProfile;

import orc.CaughtEvent;
import orc.DirectInvoker;
import orc.Invoker;
import orc.error.runtime.ExceptionHaltException;
import orc.error.runtime.HaltException;
import orc.run.porce.SpecializationConfiguration;
import orc.run.porce.runtime.PorcEExecution;
import orc.run.porce.runtime.PorcERuntime;

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
			// TODO: Wrap exception to include Orc stack information. This will mean wrapping this in JavaException if needed and calling setBacktrace
			execution.notifyOrcWithBoundary(new CaughtEvent(e.getCause()));
			throw HaltException.SINGLETON();
		} catch (final HaltException e) {
			exceptionProfiles[1].enter();
			throw e;
		} catch (final Exception e) {
			exceptionProfiles[2].enter();
			// TODO: Wrap exception to include Orc stack information. This will mean wrapping this in JavaException if needed and calling setBacktrace
			execution.notifyOrcWithBoundary(new CaughtEvent(e));
			throw HaltException.SINGLETON();
		} catch (final Throwable e) {
			CompilerDirectives.transferToInterpreter();
			// TODO: Wrap exception to include Orc stack information. This will mean wrapping this in JavaException if needed and calling setBacktrace
			execution.notifyOrcWithBoundary(new CaughtEvent(e));
			// Rethrow into interpreter since this is an error and everything is exploding.
			//throw e;
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
