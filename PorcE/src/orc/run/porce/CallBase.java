package orc.run.porce;

import java.util.Arrays;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import orc.run.porce.InternalCall.Specific;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecutionRef;
import orc.run.porce.runtime.PorcERuntime;

public abstract class CallBase extends Expression {
	@Children
	protected final Expression[] arguments;

	protected final PorcEExecutionRef execution;

	@Child
	protected Expression target;

	protected CallBase(Expression target, Expression[] arguments, PorcEExecutionRef execution) {
		this.target = target;
		this.arguments = arguments;
		this.execution = execution;
	}

	@ExplodeLoop
	public void executeArguments(Object[] argumentValues, final int valueOffset, final int argOffset,
			VirtualFrame frame) {
		assert (argumentValues.length == arguments.length - argOffset + valueOffset);
		for (int i = 0; i < arguments.length - argOffset; i++) {
			argumentValues[i + valueOffset] = arguments[i + argOffset].execute(frame);
		}
	}

	public PorcEClosure executeTargetClosure(VirtualFrame frame) {
		try {
			return target.executePorcEClosure(frame);
		} catch (UnexpectedResultException e) {
			throw InternalPorcEError.typeError(this, e);
		}
	}

	public Object executeTargetObject(VirtualFrame frame) {
		return target.execute(frame);
	}

	public PorcERuntime getRuntime() {
		return execution.get().runtime();
	}

	public static Expression[] copyExpressionArray(Expression[] arguments) {
		return Arrays.stream(arguments).map(n -> n.copy()).toArray((n) -> new Expression[n]);
	}
}