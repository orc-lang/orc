
package orc.run.porce;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecutionRef;

public class Call extends Expression {
    @Child
    protected CallBase internalCall;
    @Child
    protected CallBase externalCall;
    @Child
    protected Expression target;

    private final ConditionProfile profiler = ConditionProfile.createBinaryProfile();

    public Call(final Expression target, final Expression[] arguments, final PorcEExecutionRef execution, final CallBase externalCall) {
        this.target = (Expression) target.copy();
        this.internalCall = InternalCall.create((Expression) target.copy(), CallBase.copyExpressionArray(arguments), execution);
        this.externalCall = externalCall;
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

        if (profiler.profile(isInternal(target.execute(frame)))) {
            return internalCall.execute(frame);
        } else {
            return externalCall.execute(frame);
        }
    }

    public static class Direct {
        public static Call create(final Expression target, final Expression[] arguments, final PorcEExecutionRef execution) {
            return new Call(target, arguments, execution, ExternalDirectCall.create(target, arguments, execution));
        }
    }

    public static class CPS {
        public static Call create(final Expression target, final Expression[] arguments, final PorcEExecutionRef execution) {
            return new Call(target, arguments, execution, ExternalCPSCall.create(target, arguments, execution));
        }
    }

    public static boolean isInternal(final Object t) {
        return t instanceof PorcEClosure;
    }
}
