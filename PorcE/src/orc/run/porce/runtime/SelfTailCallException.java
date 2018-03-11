//
// SelfTailCallException.java -- Java exception SelfTailCallException
// Project PorcE
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.runtime;

import com.oracle.truffle.api.nodes.ControlFlowException;

/**
 * An internal exception used by PorcE to unwind the interpreter stack back to the entry to the current function (RootNode).
 * 
 * @author amp
 */
@SuppressWarnings("serial")
public final class SelfTailCallException extends ControlFlowException {
    /* Just used to unwind the stack */
}
