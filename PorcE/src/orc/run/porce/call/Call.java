
package orc.run.porce.call;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.profiles.ConditionProfile;

import orc.run.porce.Expression;
import orc.run.porce.PorcEUnit;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecutionRef;

public abstract class Call<ExternalDispatch extends Dispatch> extends Expression {
	@Child
	protected Dispatch internalCall = null;
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
	
	private final PorcEExecutionRef execution;

	protected Call(final Expression target, final Expression[] arguments, final PorcEExecutionRef execution) {
		this.target = target;
		this.arguments = arguments;
		this.execution = execution;
	}

	protected InternalCPSDispatch makeInternalCall() {
		return InternalCPSDispatch.create(execution);
	}

	protected Dispatch getInternalCall() {
		if (internalCall == null) {
			CompilerDirectives.transferToInterpreterAndInvalidate();
			this.internalCall = insert(makeInternalCall());
			this.internalCall.setTail(isTail);
		}
		return internalCall;
	}

	protected abstract ExternalDispatch makeExternalCall();
	protected abstract Object callExternal(VirtualFrame frame, Object target, Object[] arguments);

	protected ExternalDispatch getExternalCall() {
		if (externalCall == null) {
			CompilerDirectives.transferToInterpreterAndInvalidate();
			this.externalCall = insert(makeExternalCall());
			this.externalCall.setTail(isTail);
		}
		return externalCall;
	}

	protected InterceptedDispatch makeInterceptedCall() {
		return InterceptedDispatch.create(execution);
	}

	protected Dispatch getInterceptedCall() {
		if (interceptedCall == null) {
			CompilerDirectives.transferToInterpreterAndInvalidate();
			this.interceptedCall = insert(makeInterceptedCall());
			this.interceptedCall.setTail(isTail);
		}
		return interceptedCall;
	}

	@Override
	public Object execute(final VirtualFrame frame) {
		final Object targetValue = executeTargetObject(frame);
		final Object[] argumentValues = new Object[arguments.length];
		executeArguments(argumentValues, frame);

		if (profileIsIntercepted.profile(execution.get().shouldInterceptInvocation(targetValue, argumentValues))) {
			getInterceptedCall().executeDispatch(frame, targetValue, argumentValues);
		} else if (profileIsInternal.profile(isInternal(targetValue))) {
			getInternalCall().executeDispatch(frame, targetValue, argumentValues);
		} else {
			return callExternal(frame, targetValue, argumentValues);
		}
		return PorcEUnit.SINGLETON;
	}

    public static class Direct {
        public static Expression create(final Expression target, final Expression[] arguments, final PorcEExecutionRef execution) {
            return new Call<DirectDispatch>(target, arguments, execution) {
                @Override
                protected DirectDispatch makeExternalCall() {
                       return ExternalDirectDispatch.create(execution);
                }

				@Override
				protected Object callExternal(VirtualFrame frame, Object target, Object[] arguments) {
					return getExternalCall().executeDirectDispatch(frame, target, arguments);
				}
            };
       }
   }

   public static class CPS {
       public static Expression create(final Expression target, final Expression[] arguments, final PorcEExecutionRef execution) {
            return CatchTailCall.create(new Call<Dispatch>(target, arguments, execution) {
                @Override
                protected Dispatch makeExternalCall() {
                       return ExternalCPSDispatch.create(execution);
                }

				@Override
				protected Object callExternal(VirtualFrame frame, Object target, Object[] arguments) {
					getExternalCall().executeDispatch(frame, target, arguments);
					return PorcEUnit.SINGLETON;
				}
            }, execution);
        }
	}

	@ExplodeLoop
	public void executeArguments(final Object[] argumentValues, final VirtualFrame frame) {
		assert argumentValues.length == arguments.length;
		for (int i = 0; i < arguments.length; i++) {
			argumentValues[i] = arguments[i].execute(frame);
		}
	}
	
    public Object executeTargetObject(final VirtualFrame frame) {
        return target.execute(frame);
    }

	public static boolean isInternal(final Object t) {
		return t instanceof PorcEClosure;
	}
}
