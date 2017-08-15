
package orc.run.porce.runtime;

import scala.Function1;

import com.oracle.truffle.api.RootCallTarget;
import orc.run.distrib.DOrcMarshalingReplacement;

final public class PorcEClosure implements DOrcMarshalingReplacement {
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

    @Override
    public boolean isReplacementNeededForMarshaling(final Function1<Object, Object> marshalValueWouldReplace) {
        return JavaMarshalingUtilities.existsMarshalValueWouldReplace(environment, marshalValueWouldReplace);
    }

    @Override
    public Object replaceForMarshaling(final Function1<Object, Object> marshaler) {
        return new PorcEClosure(JavaMarshalingUtilities.mapMarshaler(environment, marshaler), body, isRoutine);
    }

    @Override
    public boolean isReplacementNeededForUnmarshaling(final Function1<Object, Object> unmarshalValueWouldReplace) {
        return JavaMarshalingUtilities.existsMarshalValueWouldReplace(environment, unmarshalValueWouldReplace);
    }

    @Override
    public Object replaceForUnmarshaling(final Function1<Object, Object> unmarshaler) {
		return new PorcEClosure(JavaMarshalingUtilities.mapMarshaler(environment, unmarshaler), body, isRoutine);
	}
}
