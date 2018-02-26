package orc.run.porce.call;

import orc.run.porce.runtime.PorcEExecution;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Instrumentable;

@Instrumentable(factory = DispatchWrapper.class)
public abstract class Dispatch extends DispatchBase {
	protected Dispatch(final PorcEExecution execution) {
		super(execution);
	}
	
	protected Dispatch(final Dispatch orig) {
		super(orig.execution);
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
