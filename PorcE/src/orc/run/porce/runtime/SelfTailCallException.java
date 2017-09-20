package orc.run.porce.runtime;

import com.oracle.truffle.api.nodes.ControlFlowException;

@SuppressWarnings("serial")
public final class SelfTailCallException extends ControlFlowException {
  /* Just used to unwind the stack */
}
