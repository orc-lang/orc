//
// InlinedTailCallException.java -- Java class InlinedTailCallException
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.runtime;

import orc.run.porce.PorcERootNode;

import com.oracle.truffle.api.nodes.ControlFlowException;

/**
 * An internal PorcE exception which is used to implement TCO in inlined functions.
 *
 * @author amp
 */
@SuppressWarnings("serial")
public final class InlinedTailCallException extends ControlFlowException {
    public final PorcERootNode target;

    public InlinedTailCallException(PorcERootNode target) {
        this.target = target;
    }
}
