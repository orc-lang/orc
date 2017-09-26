
package orc.run.porce.call;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
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
		return InternalCPSDispatch.createBare(execution);
	}

	protected Dispatch getInternalCall() {
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

	@Override
	public Object execute(final VirtualFrame frame) {
		final Object targetValue = executeTargetObject(frame);
		final Object[] argumentValues = new Object[arguments.length];
		executeArguments(argumentValues, frame);
		
		//clearFrameIfTail(frame);

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
                       return ExternalDirectDispatch.createBare(execution);
                }

				@Override
				protected Object callExternal(VirtualFrame frame, Object target, Object[] arguments) {
					return getExternalCall().executeDirectDispatch(frame, target, arguments);
				}
            };
       }
   }

   public static class CPS {
       public static Expression create(final Expression target, final Expression[] arguments, final PorcEExecutionRef execution, boolean isTail) {
    	   if (isTail) {
    		   return createTail(target, arguments, execution);
    	   } else {
    		   return createNontail(target, arguments, execution);
    	   }
       }
       public static Expression createNontail(final Expression target, final Expression[] arguments, final PorcEExecutionRef execution) {
           return CatchTailCall.create(createTail(target, arguments, execution), execution);
       }
       public static Expression createTail(final Expression target, final Expression[] arguments, final PorcEExecutionRef execution) {
           return new Call<Dispatch>(target, arguments, execution) {
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

	@ExplodeLoop
	public void executeArguments(final Object[] argumentValues, final VirtualFrame frame) {
		assert argumentValues.length == arguments.length;
		for (int i = 0; i < arguments.length; i++) {
			argumentValues[i] = arguments[i].execute(frame);
		}
	}

	//@ExplodeLoop
	protected void clearFrameIfTail(VirtualFrame frame) {
		if(isTail) {
			FrameDescriptor descriptor = frame.getFrameDescriptor();
			//CompilerAsserts.partialEvaluationConstant(descriptor);
			//CompilerAsserts.partialEvaluationConstant(descriptor.getSlots());
			for(FrameSlot slot : descriptor.getSlots()) {
				//CompilerAsserts.partialEvaluationConstant(slot);
				if(slot.getKind() == FrameSlotKind.Object) {
					frame.setObject(slot, null);
				}
			}
		}
	}

    public Object executeTargetObject(final VirtualFrame frame) {
        return target.execute(frame);
    }

	public static boolean isInternal(final Object t) {
		return t instanceof PorcEClosure;
	}
}