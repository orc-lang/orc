package orc.run.porce.runtime;

import com.oracle.truffle.api.RootCallTarget;

// TODO: Could this usefully be a @ValueType?
final public class PorcEClosure {
	public final Object[] capturedValues;
	public final RootCallTarget body;
	
	public final boolean isRoutine;
	
	// TODO: PERFORMANCE: Using a frame instead of an array for captured values may perform better. Though that will mainly be true when we start using native values.
	public PorcEClosure(Object[] capturedValues, RootCallTarget body, boolean isRoutine, Terminator executionTerminator) {
		this.capturedValues = capturedValues;
		this.body = body;
		this.isRoutine = isRoutine;
	}

	public Object callFromRuntimeArgArray(Object[] values) {
		values[0] = capturedValues;
		return body.call(values);
	}

	public Object callFromRuntime() {
		return body.call((Object)capturedValues);
	}
	public Object callFromRuntime(Object p1) {
		return body.call(capturedValues, p1);
	}
	public Object callFromRuntime(Object p1, Object p2) {
		return body.call(capturedValues, p1, p2);
	}
	public Object callFromRuntime(Object p1, Object p2, Object p3) {
		return body.call(capturedValues, p1, p2, p3);
	}

	public Object callFromRuntimeVarArgs(Object[] args) {
		Object[] values = new Object[args.length + 1];
		values[0] = capturedValues;
		System.arraycopy(args, 0, values, 1, args.length);
		return body.call(values);
	}
	
	public static PorcEClosure create(Object[] capturedValues, RootCallTarget body, boolean isRoutine, Terminator executionTerminator) {
		return new PorcEClosure(capturedValues, body, isRoutine, executionTerminator);
	}
}
