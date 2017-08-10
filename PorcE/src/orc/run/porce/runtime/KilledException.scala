package orc.run.porce.runtime

import com.oracle.truffle.api.nodes.ControlFlowException

/** Notify the enclosing code that this Orc code has been killed.
  *
  * This is thrown by checkLive() and caught in Trim implementations.
  */
final class KilledException extends ControlFlowException
