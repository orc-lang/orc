package orc.run.porce.call;

import orc.run.porce.NodeBase;
import orc.run.porce.runtime.PorcEExecution;

public abstract class DispatchBase extends NodeBase {
	protected final PorcEExecution execution;

	protected DispatchBase(PorcEExecution execution) {
		this.execution = execution;
	}
}
