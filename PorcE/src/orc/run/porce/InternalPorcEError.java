
package orc.run.porce;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

public class InternalPorcEError extends Error {
    private static final long serialVersionUID = 269299963113819157L;

    public InternalPorcEError(final Throwable e) {
        super(e);
    }

    public InternalPorcEError(final String msg, final Throwable e) {
        super(msg, e);
    }

    public InternalPorcEError(final String msg) {
        super(msg);
    }

    public static InternalPorcEError typeError(final PorcENode n, final UnexpectedResultException e) {
        CompilerDirectives.transferToInterpreter();
        throw new InternalPorcEError("Received illegal value '" + e.getResult() + "' as some parameter in '" + n.porcNode() + "'.", e);
    }

    public static InternalPorcEError typeError(final PorcENode n, final Exception e) {
        CompilerDirectives.transferToInterpreter();
        throw new InternalPorcEError("Received illegal value '" + e + "' as some parameter in '" + n.porcNode() + "'.", e);
    }

    @TruffleBoundary(allowInlining = true)
    public static InternalPorcEError capturedLengthError(final int slotsLen, final int capturedsLen) {
        throw new InternalPorcEError("captureds array is the wrong length: expected len = " + slotsLen + ", provided len = " + capturedsLen);
    }

    public static InternalPorcEError unreachable(final PorcENode n) {
        CompilerDirectives.transferToInterpreter();
        throw new InternalPorcEError("Code should be unreachable in " + n.porcNode() + "'.");
    }
}
