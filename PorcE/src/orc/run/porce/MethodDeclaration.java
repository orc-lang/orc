package orc.run.porce;

import java.util.Arrays;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import orc.run.porce.runtime.PorcEClosure;

public class MethodDeclaration extends Expression {
	@Children
	protected final Expression[] capturingVariables;
	@Children
	protected final Method[] methods;
	@Child
	protected Expression body;

	public MethodDeclaration(Method[] methods, Expression body) {
		FrameSlot[] capturingSlots = methods[0].getCapturingSlots();
		for (int i = 0; i < methods.length; i++) {
			Method method = methods[i];
			assert Arrays.deepEquals(capturingSlots, method.getCapturingSlots());
		}
		
		this.body = body;
		this.methods = methods;
		this.capturingVariables = new Expression[capturingSlots.length];
		for (int i = 0; i < capturingSlots.length; i++) {
			capturingVariables[i] = new Variable(capturingSlots[i]);
		}
	}
	
	@ExplodeLoop
	public Object execute(VirtualFrame frame) {
		Object[] capturedValues = new Object[capturingVariables.length + methods.length];
		for (int i = 0; i < capturingVariables.length; i++) {
			capturedValues[i] = capturingVariables[i].execute(frame);
		}
		for (int i = 0; i < methods.length; i++) {
			PorcEClosure closure = methods[i].getClosure(capturedValues);
			capturedValues[capturingVariables.length + i] = closure;
			frame.setObject(methods[i].getName(), closure);
		}
		return body.execute(frame);
	}

	public static MethodDeclaration create(Method[] methods, Expression body) {
		return new MethodDeclaration(methods, body);
	}
}