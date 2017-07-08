package orc.run.porce;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

public class Continuation extends Expression {
	//protected final FrameSlot[] argumentSlots;
	//protected final FrameSlot[] capturedSlots;

	@Children
	protected final Expression[] capturedVariables;
	private final RootCallTarget callTarget;

	public Continuation(FrameSlot[] argumentSlots, FrameSlot[] capturedSlots, Expression body, FrameDescriptor descriptor) {
		this.capturedVariables = new Expression[capturedSlots.length];
		for (int i = 0; i < capturedSlots.length; i++) {
			capturedVariables[i] = new Variable(capturedSlots[i]);
		}
		ContinuationRootNode rootNode = new ContinuationRootNode(argumentSlots, capturedSlots, descriptor, body);
		this.callTarget = Truffle.getRuntime().createCallTarget(rootNode);
	}
	
	public CallTarget getCallTarget() {
		return callTarget;
	}
	
	@ExplodeLoop
	public Object execute(VirtualFrame frame) {
		Object[] capturedValues = new Object[capturedVariables.length];
		for (int i = 0; i < capturedVariables.length; i++) {
			capturedValues[i] = capturedVariables[i].execute(frame);
		}
		return new PorcEContinuationClosure(capturedValues, callTarget);
	}

	public static Continuation create(FrameSlot[] argumentSlots, FrameSlot[] capturedSlots, Expression body, FrameDescriptor descriptor) {
		return new Continuation(argumentSlots, capturedSlots, body, descriptor);
	}
}