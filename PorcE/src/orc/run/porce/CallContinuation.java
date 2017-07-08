package orc.run.porce;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.UnsupportedSpecializationException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

public class CallContinuation extends Expression {
	@Child
	protected Expression target;
	@Children
	protected final Expression[] arguments;
	@Child
	protected IndirectCallNode callNode;

	public CallContinuation(Expression target, Expression[] arguments) {
		this.target = target;
		this.arguments = arguments;
		this.callNode = Truffle.getRuntime().createIndirectCallNode();
	}

	@ExplodeLoop
	public Object execute(VirtualFrame frame) {
		PorcEContinuationClosure t;
		try {
			t = target.executePorcEContinuationClosure(frame);
		} catch (UnexpectedResultException e) {
			throw new UnsupportedSpecializationException(this, new Node[] { this.target }, e);
		}
		Object[] argumentValues = new Object[arguments.length+1];
		argumentValues[0] = t.capturedValues;
		for (int i = 0; i < arguments.length; i++) {
			argumentValues[i+1] = arguments[i].execute(frame);
		}
		return callNode.call(t.body, argumentValues);
	}

	public static CallContinuation create(Expression target, Expression[] arguments) {
		return new CallContinuation(target, arguments);
	}
}
