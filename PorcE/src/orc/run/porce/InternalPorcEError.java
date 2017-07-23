package orc.run.porce;

import static com.oracle.truffle.api.CompilerDirectives.*;

import com.oracle.truffle.api.nodes.UnexpectedResultException;

public class InternalPorcEError extends Error {
	private static final long serialVersionUID = 269299963113819157L;

	public InternalPorcEError(Throwable e) {
		super(e);
	}

	public InternalPorcEError(String msg, Throwable e) {
		super(msg, e);
	}

	public InternalPorcEError(String msg) {
		super(msg);
	}

	public static InternalPorcEError typeError(PorcENode n, UnexpectedResultException e) {
		transferToInterpreter();
		throw new InternalPorcEError("Received illegal value '" + e.getResult() + "' as some parameter in '" + n + "'.",
				e);
	}

	public static InternalPorcEError capturedLengthError(int slotsLen, int capturedsLen) {
		transferToInterpreter();
		throw new InternalPorcEError(
				"captureds array is the wrong length: slots len = " + slotsLen + ", captureds len = " + capturedsLen);
	}
}
