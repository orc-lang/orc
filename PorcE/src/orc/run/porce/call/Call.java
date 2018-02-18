
package orc.run.porce.call;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.profiles.ConditionProfile;

import orc.run.porce.Expression;
import orc.run.porce.PorcEUnit;
import orc.run.porce.RuntimeProfilerWrapper;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecution;

public abstract class Call<ExternalDispatch extends Dispatch> extends Expression {
	@Child
	protected InternalCPSDispatch internalCall = null;
	@Child
	protected ExternalDispatch externalCall = null;
	@Child
	protected Dispatch interceptedCall = null;

	private final ConditionProfile profileIsInternal = ConditionProfile.createBinaryProfile();
	private final ConditionProfile profileIsIntercepted = ConditionProfile.createBinaryProfile();
	
	@Child
	private Expression target;
	@Children
	private final Expression[] arguments;
	
	private final PorcEExecution execution;

	protected Call(final Expression target, final Expression[] arguments, final PorcEExecution execution) {
		this.target = target;
		this.arguments = arguments;
		this.execution = execution;
	}
	
	@Override
	public void setTail(boolean v) {
		super.setTail(v);
		if (interceptedCall != null)
			interceptedCall.setTail(v);
		if (externalCall != null)
			externalCall.setTail(v);
		if (internalCall != null)
			internalCall.setTail(v);
	}

	protected InternalCPSDispatch makeInternalCall() {
		return InternalCPSDispatch.createBare(execution);
	}

	protected InternalCPSDispatch getInternalCall() {
		if (internalCall == null) {
			CompilerDirectives.transferToInterpreterAndInvalidate();
			computeAtomicallyIfNull(() -> internalCall, (v) -> this.internalCall = v, () -> {
				InternalCPSDispatch n = insert(makeInternalCall());
				n.setTail(isTail);
				return n;
			});
		}
		return internalCall;
	}

	protected abstract ExternalDispatch makeExternalCall();
	protected abstract Object callExternal(VirtualFrame frame, Object target, Object[] arguments);

	protected ExternalDispatch getExternalCall() {
		if (externalCall == null) {
			CompilerDirectives.transferToInterpreterAndInvalidate();
			computeAtomicallyIfNull(() -> externalCall, (v) -> this.externalCall = v, () -> {
				ExternalDispatch n = insert(makeExternalCall());
				n.setTail(isTail);
				return n;
			});
		}
		return externalCall;
	}

	protected InterceptedDispatch makeInterceptedCall() {
		return InterceptedDispatch.create(execution);
	}

	protected Dispatch getInterceptedCall() {
		if (interceptedCall == null) {
			CompilerDirectives.transferToInterpreterAndInvalidate();
			computeAtomicallyIfNull(() -> interceptedCall, (v) -> this.interceptedCall = v, () -> {
				InterceptedDispatch n = insert(makeInterceptedCall());
				n.setTail(isTail);
				return n;
			});
		}
		return interceptedCall;
	}

	private static final Object[] emptyArguments = new Object[0];
	
	@Override
	public Object execute(final VirtualFrame frame) {
		final Object targetValue = executeTargetObject(frame);
		final Object[] argumentValues;
		if (!profileIsInternal.profile(isInternal(targetValue))) {
			RuntimeProfilerWrapper.traceEnter(RuntimeProfilerWrapper.CallDispatch, getCallSiteId());
			// The traceExit is in Direct below and in ExternalCPSDispatch
		}
		if (arguments.length > 0) {
			argumentValues = new Object[arguments.length];
			executeArguments(frame, argumentValues, 0);
		} else {
			argumentValues = emptyArguments;
		}

		if (profileIsIntercepted.profile(execution.shouldInterceptInvocation(targetValue, argumentValues))) {
			getInterceptedCall().executeDispatch(frame, targetValue, argumentValues);
		} else if (profileIsInternal.profile(isInternal(targetValue))) {
			final Object[] argumentValuesI = new Object[arguments.length + 1];
			executeArguments(frame, argumentValuesI, 1);
			argumentValuesI[0] = ((PorcEClosure) targetValue).environment;

			getInternalCall().executeDispatchWithEnvironment(frame, targetValue, argumentValuesI);
		} else {
			return callExternal(frame, targetValue, argumentValues);
		}
		return PorcEUnit.SINGLETON;
	}

	public static class Direct {
		public static Expression create(final Expression target, final Expression[] arguments, final PorcEExecution execution) {
			return new Call<DirectDispatch>(target, arguments, execution) {
				{
					// TODO: Remove once I've debugged performance issues.
					getExternalCall();
				}
				
				@Override
				protected DirectDispatch makeExternalCall() {
					return ExternalDirectDispatch.createBare(execution);
				}

				@Override
				protected Object callExternal(VirtualFrame frame, Object target, Object[] arguments) {
					try {
						return getExternalCall().executeDirectDispatch(frame, target, arguments);
					} finally {
						RuntimeProfilerWrapper.traceExit(RuntimeProfilerWrapper.CallDispatch, getCallSiteId());
					}
				}
			};
		}
	}

	public static class CPS {
		public static Expression create(final Expression target, final Expression[] arguments,
				final PorcEExecution execution, boolean isTail) {
			if (isTail) {
				return createTail(target, arguments, execution);
			} else {
				return createNontail(target, arguments, execution);
			}
		}

		public static Expression createNontail(final Expression target, final Expression[] arguments,
				final PorcEExecution execution) {
			return CatchTailCall.create(createTail(target, arguments, execution), execution);
		}

		public static Expression createTail(final Expression target, final Expression[] arguments,
				final PorcEExecution execution) {
			return new Call<Dispatch>(target, arguments, execution) {
				{
					// TODO: Remove once I've debugged performance issues.
					getExternalCall();
					getInternalCall();
				}
				
				@Override
				protected Dispatch makeExternalCall() {
					return ExternalCPSDispatch.createBare(execution);
				}

				@Override
				protected Object callExternal(VirtualFrame frame, Object target, Object[] arguments) {
					getExternalCall().executeDispatch(frame, target, arguments);
					return PorcEUnit.SINGLETON;
				}
			};
		}
	}

	@ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.FULL_UNROLL)
	public void executeArguments(final VirtualFrame frame, final Object[] argumentValues, int offset) {
		assert argumentValues.length - offset == arguments.length;
		for (int i = 0; i < arguments.length; i++) {
			argumentValues[i + offset] = arguments[i].execute(frame);
		}
	}

    public Object executeTargetObject(final VirtualFrame frame) {
        return target.execute(frame);
    }

	public static boolean isInternal(final Object t) {
		return t instanceof PorcEClosure;
	}
}
