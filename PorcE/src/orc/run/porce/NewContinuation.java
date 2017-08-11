package orc.run.porce;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.RootNode;

import orc.ast.porc.PorcAST;
import orc.run.porce.runtime.PorcEClosure;

public class NewContinuation extends Expression {
	@Children
	protected final Expression[] capturedVariables;
	protected final RootCallTarget callTarget;
	
	public void setPorcAST(PorcAST ast) {
		((PorcERootNode)callTarget.getRootNode()).setPorcAST(ast);
		super.setPorcAST(ast);
	}

	public NewContinuation(Expression[] capturedVariables, RootNode rootNode) {
		this.capturedVariables = capturedVariables;
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

	public static NewContinuation create(Expression[] capturedVariables, RootNode rootNode) {
		return new NewContinuation(capturedVariables, rootNode);
	}
}