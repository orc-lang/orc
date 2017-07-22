package orc.run.porce;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import orc.ast.porc.PorcAST;
import orc.run.porce.runtime.PorcEClosure;

public class Continuation extends Expression {
	@Children
	protected final Expression[] capturedVariables;
	protected RootCallTarget callTarget;
	
	public void setPorcAST(PorcAST ast) {
		((PorcERootNode)callTarget.getRootNode()).setPorcAST(ast);
		super.setPorcAST(ast);
	}

	public Continuation(FrameSlot[] argumentSlots, FrameSlot[] capturedSlots, FrameSlot[] capturingSlots, FrameDescriptor descriptor, Expression body) {
		this.capturedVariables = new Expression[capturedSlots.length];
		for (int i = 0; i < capturedSlots.length; i++) {
			capturedVariables[i] = new Variable(capturedSlots[i]);
		}
		PorcERootNode rootNode = new PorcERootNode(argumentSlots, capturingSlots, descriptor, body);
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
		/*Logger.info(() -> 
			"Allocating closure with " + capturedVariables.length + " captured variables and initial values " + 
			(capturedValues.length > 0 ? capturedValues[0] : null) + ", " + 
			(capturedValues.length > 1 ? capturedValues[1] : null) + ", " +  
			(capturedValues.length > 2 ? capturedValues[2] : null) + ", " +  
			(capturedValues.length > 3 ? capturedValues[3] : null) + ", " +  
			(capturedValues.length > 4 ? capturedValues[4] : null));*/
		return new PorcEClosure(capturedValues, callTarget, false);
	}

	public static Continuation create(FrameSlot[] argumentSlots, FrameSlot[] capturedSlots, FrameSlot[] capturingSlots, FrameDescriptor descriptor, Expression body) {
		return new Continuation(argumentSlots, capturedSlots, capturingSlots, descriptor, body);
	}
}