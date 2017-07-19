package orc.run.porce.runtime;

import com.oracle.truffle.api.RootCallTarget;

// TODO: Could this usefully be a @ValueType
final public class PorcEClosure {
	final public Object[] capturedValues;
	final public RootCallTarget body;
	public final boolean isDef;

	// TODO: PERFORMANCE: Using a frame instead of an array for captured values may perform better. Though that will mainly be true when we start using native values.
	public PorcEClosure(Object[] capturedValues, RootCallTarget body, boolean isDef) {
		this.capturedValues = capturedValues;
		this.body = body;
		this.isDef = isDef;
	}
	
	public Object callFromRuntime(Object... args) {
		Object[] values = new Object[args.length + 1];
		values[0] = capturedValues;
		System.arraycopy(args, 0, values, 1, args.length);
		return body.call(values);
	}
	
	public static PorcEClosure create(Object[] capturedValues, RootCallTarget body, boolean isDef) {
		return new PorcEClosure(capturedValues, body, isDef);
	}
}
