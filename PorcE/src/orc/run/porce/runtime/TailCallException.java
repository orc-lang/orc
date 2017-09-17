package orc.run.porce.runtime;

import com.oracle.truffle.api.nodes.ControlFlowException;

@SuppressWarnings("serial")
public final class TailCallException extends ControlFlowException {
	public final PorcEClosure target;
	public final Object[] arguments;

	public TailCallException(PorcEClosure target, Object[] arguments) {
		this.target = target;
		this.arguments = arguments;
	}
}
