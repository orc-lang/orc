package orc.run.porce;

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

	public Object executeWithValues(final VirtualFrame frame, final Object target, final Object[] arguments) {
		final PorcEClosure pub = (PorcEClosure) arguments[0];
		final Counter counter = (Counter) arguments[1];
		final Terminator term = (Terminator) arguments[2];

		// Token: Passed to handle from arguments.
		final CPSCallResponseHandler handle = new CPSCallResponseHandler(execution.get(), pub, counter, term, getCallSiteId());

		execution.get().invokeIntercepted(handle, target, buildArgumentValues(arguments));
		return PorcEUnit.SINGLETON;
	}

	@Override
	public Object execute(final VirtualFrame frame) {
		final Object targetValue = executeTargetClosure(frame);
		final Object[] argumentValues = new Object[arguments.length];
		executeArguments(argumentValues, 0, 0, frame);

		return executeWithValues(frame, targetValue, argumentValues);
	}

	protected Object[] buildArgumentValues(final Object[] arguments) {
		final Object[] argumentValues = new Object[arguments.length - 3];
		System.arraycopy(arguments, 3, argumentValues, 0, argumentValues.length);
		return argumentValues;
	}

	public static InterceptedCall create(Expression target, Expression[] arguments, PorcEExecutionRef execution) {
		return new InterceptedCall(target, arguments, execution);
	}
}
