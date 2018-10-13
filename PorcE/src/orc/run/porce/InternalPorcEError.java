//
// InternalPorcEError.java -- Java class InternalPorcEError
// Project PorcE
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

/**
 * An exception representing an internal non-recoverable error in PorcE.
 *
 * This class also has utility functions to throw the internal exceptions.
 *
 * @author amp
 */
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

    public static InternalPorcEError capturedLengthError(final int slotsLen, final int capturedsLen) {
        CompilerDirectives.transferToInterpreter();
        throw new InternalPorcEError("captureds array is the wrong length: expected len = " + slotsLen + ", provided len = " + capturedsLen);
    }

    public static InternalPorcEError unreachable(final PorcENode n) {
        CompilerDirectives.transferToInterpreter();
        throw new InternalPorcEError("Code should be unreachable in " + n.porcNode() + "'.");
    }
}
