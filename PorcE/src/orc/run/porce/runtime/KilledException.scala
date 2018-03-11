//
// KilledException.scala -- Scala exception KilledException
// Project PorcE
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.runtime

import com.oracle.truffle.api.nodes.ControlFlowException

/** Notify the enclosing code that this Orc code has been killed.
  *
  * This is thrown by checkLive() and caught in Trim implementations.
  */
final class KilledException extends ControlFlowException
