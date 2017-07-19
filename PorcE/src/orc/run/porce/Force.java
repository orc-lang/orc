package orc.run.porce;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import orc.run.porce.runtime.Counter;
import orc.run.porce.runtime.Terminator;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecution;

public class Force extends Expression {
	private final PorcEExecution execution;
	@Child
	protected Expression p;
	@Child
	protected Expression c;
	@Child
	protected Expression t;
	@Children
	protected final Expression[] futures;
	
	public Force(Expression p, Expression c, Expression t, Expression[] futures, PorcEExecution execution) {
		this.p = p;
		this.c = c;
		this.t = t;
		this.futures = futures;
		this.execution = execution;
	}

	@ExplodeLoop
	public void executePorcEUnit(VirtualFrame frame) {
		try {
			final PorcEClosure pValue = p.executePorcEClosure(frame);
			final Counter cValue = c.executeCounter(frame);
			final Terminator tValue = t.executeTerminator(frame);
			final Object[] futureValues = new Object[futures.length];
			for (int i = 0; i < futures.length; i++) {
				futureValues[i] = futures[i].execute(frame);
			}
			// FIXME: PERFORMANCE: Make .force (below) execute a normal PorcE Call node. This would allow caching and inlining of the already resolved case.
			// TODO: PERFORMANCE: Use a loop (in Java) to go over all the futures. This will allow ExplodeLoop since the length of .futures is known.
			// TODO: PERFORMANCE: Speculate that the future state is stably stop or a value (maybe giving it a few tries to stabilize for initial accesses).
			execution.runtime().force(pValue, cValue, tValue, futureValues);
		} catch (UnexpectedResultException e) {
			InternalPorcEError.typeError(this, e);
		}
	}
	
	public static Force create(Expression p, Expression c, Expression t, Expression[] futures, PorcEExecution execution) {
		return new Force(p, c, t, futures, execution);
	}
}
