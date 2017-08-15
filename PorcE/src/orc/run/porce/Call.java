
package orc.run.porce;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;

import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecutionRef;

public abstract class Call extends CallBase {
	@Child
	protected CallBase internalCall = null;
	@Child
	protected CallBase externalCall = null;
	@Child
	protected InterceptedCall interceptedCall = null;

	private final ConditionProfile profileIsInternal = ConditionProfile.createBinaryProfile();
	private final ConditionProfile profileIsIntercepted = ConditionProfile.createBinaryProfile();

	public Call(final Expression target, final Expression[] arguments, final PorcEExecutionRef execution) {
		super(target, arguments, execution);
	}

	protected CallBase makeInternalCall() {
		return InternalCall.create((Expression) target.copy(), CallBase.copyExpressionArray(arguments), execution);
	}

	protected CallBase getInternalCall() {
		if (internalCall == null) {
			CompilerDirectives.transferToInterpreterAndInvalidate();
			this.internalCall = insert(makeInternalCall());
		}
		return internalCall;
	}

	protected abstract CallBase makeExternalCall();

	protected CallBase getExternalCall() {
		if (externalCall == null) {
			CompilerDirectives.transferToInterpreterAndInvalidate();
			this.externalCall = insert(makeExternalCall());
		}
		return externalCall;
	}

	protected InterceptedCall makeInterceptedCall() {
		return InterceptedCall.create((Expression) target.copy(), CallBase.copyExpressionArray(arguments), execution);
	}

	protected InterceptedCall getInterceptedCall() {
		if (interceptedCall == null) {
			CompilerDirectives.transferToInterpreterAndInvalidate();
			this.interceptedCall = insert(makeInterceptedCall());
		}
		return interceptedCall;
	}

	@Override
	public Object execute(final VirtualFrame frame) {
		// TODO: PERFORMANCE: This will execute the target an extra time for
		// every call. If we could replace this call with the chain directly
		// that would be better.
		// That would require that all cache nodes support a common interface so
		// that they can all call into each other. It should be easy actually.

		// TODO: PERFORMANCE: Even in the single type cache situation arguments
		// and target get re-executed at each level. The optimizer might get rid
		// of it, but that need to be verified.

		// Fixing both above cases will be easiest by converting all the call code
		// to use the DSL (for caching and other things) and "execute..." methods
		// which the DSL can generate.
		
		final Object targetValue = executeTargetObject(frame);
		final Object[] argumentValues = new Object[arguments.length];
		executeArguments(argumentValues, 0, 0, frame);

		if (profileIsIntercepted.profile(execution.get().shouldInterceptInvocation(targetValue, argumentValues))) {
			return getInterceptedCall().executeWithValues(frame, targetValue, argumentValues);
		} else if (profileIsInternal.profile(isInternal(targetValue))) {
			return getInternalCall().execute(frame);
		} else {
			return getExternalCall().execute(frame);
		}
	}

    public static class Direct {
        public static Call create(final Expression target, final Expression[] arguments, final PorcEExecutionRef execution) {
            return new Call(target, arguments, execution) {
                @Override
                protected CallBase makeExternalCall() {
                       return ExternalDirectCall.create((Expression) target.copy(), CallBase.copyExpressionArray(arguments), execution);
                }
            };
       }
   }

   public static class CPS {
       public static Call create(final Expression target, final Expression[] arguments, final PorcEExecutionRef execution) {
            return new Call(target, arguments, execution) {
                @Override
                protected CallBase makeExternalCall() {
                       return ExternalCPSCall.create((Expression) target.copy(), CallBase.copyExpressionArray(arguments), execution);
                }
            };
        }
	}

	public static boolean isInternal(final Object t) {
		return t instanceof PorcEClosure;
	}
}
