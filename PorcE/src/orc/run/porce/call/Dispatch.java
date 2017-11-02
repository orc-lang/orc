package orc.run.porce.call;

import com.oracle.truffle.api.frame.VirtualFrame;

import orc.run.porce.runtime.PorcEExecutionRef;

public abstract class Dispatch extends DispatchBase {
	protected Dispatch(final PorcEExecutionRef execution) {
		super(execution);
	}
	
	/**
	 * Dispatch the call to the target with the given arguments.
	 * 
	 * @param frame
	 *            The frame we are executing in.
	 * @param target
	 *            The call target.
	 * @param arguments
	 *            The arguments to the call. This will include (P, C, T) as a
	 *            prefix for external calls and will NOT have a gap for the
	 *            environment for internal calls.
	 */
	public abstract void executeDispatch(VirtualFrame frame, Object target, Object[] arguments);
}
