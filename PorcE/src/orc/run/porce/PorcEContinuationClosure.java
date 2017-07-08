package orc.run.porce;

import com.oracle.truffle.api.RootCallTarget;

public class PorcEContinuationClosure {
	final Object[] capturedValues;
	final RootCallTarget body;

	public PorcEContinuationClosure(Object[] capturedValues, RootCallTarget body) {
		this.capturedValues = capturedValues;
		this.body = body;
	}
}
