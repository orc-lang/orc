package orc.run.porce.runtime;

import com.oracle.truffle.api.RootCallTarget;

import orc.run.distrib.DOrcMarshalingReplacement;
import scala.Function1;

final public class PorcEClosure implements DOrcMarshalingReplacement {
	public final Object[] environment;
	public final RootCallTarget body;

	public final boolean isRoutine;
	
	// TODO: PERFORMANCE: Using a frame instead of an array for captured values may perform better. Though that will mainly be true when we start using native values.
	public PorcEClosure(Object[] environment, RootCallTarget body, boolean isRoutine) {
		this.environment = environment;
		this.body = body;
		this.isRoutine = isRoutine;
	}

	public Object callFromRuntimeArgArray(Object[] values) {
		values[0] = environment;
		return body.call(values);
	}

	public Object callFromRuntime() {
		return body.call((Object) environment);
	}

	public Object callFromRuntime(Object p1) {
		return body.call(environment, p1);
	}

	public Object callFromRuntime(Object p1, Object p2) {
		return body.call(environment, p1, p2);
	}

	public Object callFromRuntime(Object p1, Object p2, Object p3) {
		return body.call(environment, p1, p2, p3);
	}

	public Object callFromRuntimeVarArgs(Object[] args) {
		Object[] values = new Object[args.length + 1];
		values[0] = environment;
		System.arraycopy(args, 0, values, 1, args.length);
		return body.call(values);
	}
	
	// FIXME: Add special case for RootCallTarget in RuntimeConnectionInputStream. That cannot be handled here since even if we had the execution we cannot store the Int index in the field of type RootCallTarget.
	//     A different replacement type could get around that, but that is not how the replacement API is designed to work.

	public boolean isReplacementNeededForMarshaling(Function1<Object, Object> marshalValueWouldReplace) {
		return JavaMarshalingUtilities.existsMarshalValueWouldReplace(environment, marshalValueWouldReplace);
	}

	public Object replaceForMarshaling(Function1<Object, Object> marshaler) {
		return new PorcEClosure(JavaMarshalingUtilities.mapMarshaler(environment, marshaler), body, isRoutine);
	}
	
	public boolean isReplacementNeededForUnmarshaling(Function1<Object, Object> unmarshalValueWouldReplace) {
		return JavaMarshalingUtilities.existsMarshalValueWouldReplace(environment, unmarshalValueWouldReplace);
	}

	public Object replaceForUnmarshaling(Function1<Object, Object> unmarshaler) {
		return new PorcEClosure(JavaMarshalingUtilities.mapMarshaler(environment, unmarshaler), body, isRoutine);
	}
}
