//
// TailCallException.java -- Java class TailCallException
// Project PorcE
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.runtime;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ControlFlowException;

/**
 * An internal PorcE exception which is used to implement universal TCO.
 *
 * This exception is throw by calls to a different function in tail positions and
 * then caught in a trampoline wrapped around the current call and dispatched to
 * the specified closures. This unwinds the stack to avoid growing the stack in
 * co-recursive situations.
 *
 * @author amp
 */
@SuppressWarnings("serial")
public final class TailCallException extends ControlFlowException {
	public PorcEClosure target;
	public Object[] arguments;

	private TailCallException(PorcEClosure target, Object[] arguments) {
		this.target = target;
		this.arguments = arguments;
	}

        public void assignFrom(TailCallException e) {
            this.target = e.target;
            // Copy at most 16 to avoid overwriting the self reference in the arguments.
            System.arraycopy(e.arguments, 0, this.arguments, 0, Math.min(e.arguments.length, 16));
            assert this.arguments.length < 17 || this.arguments[16] == this;
        }

	public static TailCallException create(PorcEClosure target) {
		// FIXME: This sets a maximum working function arity to 16.
		TailCallException tce = new TailCallException(target, new Object[17]);
		tce.arguments[16] = tce;
		return tce;
	}

	public static TailCallException create(PorcEClosure target, Object[] arguments) {
		// FIXME: This sets a maximum working function arity to 16.
		TailCallException tce = new TailCallException(target, new Object[17]);
		System.arraycopy(arguments, 0, tce.arguments, 0, arguments.length);
		tce.arguments[16] = tce;
		return tce;
	}

        public static TailCallException get(final VirtualFrame frame) {
            Object[] thisArguments = frame.getArguments();
            if (thisArguments.length == 17 && thisArguments[16] instanceof TailCallException) {
                TailCallException tce = (TailCallException) thisArguments[16];
                return tce;
            } else {
                return null;
            }
        }
}
