package orc.run.porce;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import orc.run.porce.runtime.Counter;
import orc.run.porce.runtime.CounterNested;
import orc.run.porce.runtime.PorcEExecution;

public class NewCounter extends Expression {
	private final PorcEExecution execution;
	@Child
	protected Expression parent;
	@Child
	protected Expression haltContinuation;

	public NewCounter(PorcEExecution execution, Expression parent, Expression haltContinuation) {
		this.execution = execution;
		this.parent = parent;
		this.haltContinuation = haltContinuation;
	}

	public Object execute(VirtualFrame frame) {
		return executeCounter(frame);
	}
	
	public Counter executeCounter(VirtualFrame frame) {
		try {
			return new CounterNested(execution, parent.executeCounter(frame), haltContinuation.executePorcEClosure(frame));
		} catch (UnexpectedResultException e) {
			throw new Error(e);
		}
	}
	
	public static NewCounter create(PorcEExecution execution, Expression parent, Expression haltContinuation) {
		return new NewCounter(execution, parent, haltContinuation);
	}
}
