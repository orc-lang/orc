package orc.run.porce;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;

import orc.run.porce.runtime.CPSCallResponseHandler;
import orc.run.porce.runtime.Counter;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecutionRef;
import orc.run.porce.runtime.Terminator;

public class InterceptedCall extends CallBase {
	protected InterceptedCall(Expression target, Expression[] arguments, PorcEExecutionRef execution) {
		super(target, arguments, execution);
	}

	public Object executeWithValues(final VirtualFrame frame, final Object newTarget, final Object[] newArguments) {
		final PorcEClosure pub = (PorcEClosure) newArguments[0];
		final Counter counter = (Counter) newArguments[1];
		final Terminator term = (Terminator) newArguments[2];

		// Token: Passed to handle from arguments.
		final CPSCallResponseHandler handle = new CPSCallResponseHandler(execution.get(), pub, counter, term, getCallSiteId());

		invokeInterceptedWithBoundary(handle, newTarget, newArguments);
		return PorcEUnit.SINGLETON;
	}

	@TruffleBoundary
	private void invokeInterceptedWithBoundary(final CPSCallResponseHandler handle, final Object newTarget,
			final Object[] newArguments) {
		execution.get().invokeIntercepted(handle, newTarget, buildArgumentValues(newArguments));
	}

	@Override
	public Object execute(final VirtualFrame frame) {
		final Object targetValue = executeTargetClosure(frame);
		final Object[] argumentValues = new Object[arguments.length];
		executeArguments(argumentValues, 0, 0, frame);

		return executeWithValues(frame, targetValue, argumentValues);
	}

	protected Object[] buildArgumentValues(final Object[] newArguments) {
		final Object[] argumentValues = new Object[newArguments.length - 3];
		System.arraycopy(newArguments, 3, argumentValues, 0, argumentValues.length);
		return argumentValues;
	}

	public static InterceptedCall create(Expression target, Expression[] arguments, PorcEExecutionRef execution) {
		return new InterceptedCall(target, arguments, execution);
	}
}
