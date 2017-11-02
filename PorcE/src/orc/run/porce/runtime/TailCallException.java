package orc.run.porce.runtime;

import com.oracle.truffle.api.nodes.ControlFlowException;

@SuppressWarnings("serial")
public final class TailCallException extends ControlFlowException {
	public PorcEClosure target;
	public Object[] arguments;

	private TailCallException(PorcEClosure target, Object[] arguments) {
		this.target = target;
		this.arguments = arguments;
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
}
