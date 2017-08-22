
package orc.run.porce.runtime;

import com.oracle.truffle.api.RootCallTarget;

final public class PorcEClosure {
    public final Object[] environment;
    public final RootCallTarget body;

    public final boolean isRoutine;

    // TODO: PERFORMANCE: Using a frame instead of an array for captured values may perform better. Though that will mainly be true when we start using native values.
	public PorcEClosure(final Object[] environment, final RootCallTarget body, final boolean isRoutine) {
		if (body == null) {
			throw new IllegalArgumentException("body == null");
		}
		if (environment == null) {
			throw new IllegalArgumentException("environment == null");
		}

		this.environment = environment;
		this.body = body;
		this.isRoutine = isRoutine;
	}

    public Object callFromRuntimeArgArray(final Object[] values) {
        values[0] = environment;
        return body.call(values);
    }

    public Object callFromRuntime() {
        return body.call((Object) environment);
    }

    public Object callFromRuntime(final Object p1) {
        return body.call(environment, p1);
    }

    public Object callFromRuntime(final Object p1, final Object p2) {
        return body.call(environment, p1, p2);
    }

    public Object callFromRuntime(final Object p1, final Object p2, final Object p3) {
        return body.call(environment, p1, p2, p3);
    }

    public Object callFromRuntimeVarArgs(final Object[] args) {
        final Object[] values = new Object[args.length + 1];
        values[0] = environment;
        System.arraycopy(args, 0, values, 1, args.length);
        return body.call(values);
    }

}
