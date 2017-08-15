package orc.run.porce;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import orc.ast.ASTWithIndex;
import orc.ast.porc.PorcAST;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecutionRef;
import orc.run.porce.runtime.PorcERuntime;

public abstract class CallBase extends Expression {
	@Children
	protected final Expression[] arguments;

	protected final PorcEExecutionRef execution;

	@Child
	protected Expression target;
	
	@CompilerDirectives.CompilationFinal
	private int callSiteId = -1;
	
	protected int getCallSiteId() {
		if (callSiteId >= 0) {
			return callSiteId;
		} else {
			CompilerDirectives.transferToInterpreterAndInvalidate();
			callSiteId = findCallSiteId(this);
			return callSiteId;
		}
	}
	
	private int findCallSiteId(Expression e) {
		if (e instanceof HasPorcNode && ((HasPorcNode) e).porcNode().isDefined()) {
			PorcAST ast = ((HasPorcNode) e).porcNode().get();
			if (ast instanceof ASTWithIndex && ((ASTWithIndex) ast).optionalIndex().isDefined()) {
				return (Integer) ((ASTWithIndex) ast).optionalIndex().get();
			}
		}
		Node p = e.getParent();
		if (p instanceof CallBase) {
			return ((CallBase) e).getCallSiteId();
		}
		return -1;
	}

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