package orc.run.porce;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.nodes.Node;

import orc.ast.porc.PorcAST;
import orc.run.porce.runtime.PorcEClosure;
import scala.Option;

public class Method extends Node implements HasPorcNode {
	protected RootCallTarget callTarget;
	private final FrameSlot name;
	private final FrameSlot[] capturingSlots;
	private boolean areArgsLenient;
	
	private Option<PorcAST> porcNode = Option.apply(null);
	
	public void setPorcAST(PorcAST ast) {
		((PorcERootNode)callTarget.getRootNode()).setPorcAST(ast);
		porcNode = Option.apply(ast);
	}
	
	public Option<PorcAST> porcNode() {
		return porcNode;
	}
	


	public Method(FrameSlot name, FrameSlot[] argumentSlots, FrameSlot[] capturedSlots, FrameSlot[] capturingSlots, FrameDescriptor descriptor, boolean areArgsLenient, Expression body) {
		this.name = name;
		//this.capturedSlots = capturedSlots;
		this.capturingSlots = capturingSlots;
		this.areArgsLenient = areArgsLenient;
		PorcERootNode rootNode = new PorcERootNode(argumentSlots, capturedSlots, descriptor, body);
		this.callTarget = Truffle.getRuntime().createCallTarget(rootNode);
	}

	public PorcEClosure getClosure(Object[] capturedValues) {
		return new PorcEClosure(capturedValues, callTarget, areArgsLenient);
	}
	
	public CallTarget getCallTarget() {
		return callTarget;
	}
	

	public FrameSlot getName() {
		return name;
	}
	
	public FrameSlot[] getCapturingSlots() {
		return capturingSlots;
	}

	public static Method create(FrameSlot name, FrameSlot[] argumentSlots, FrameSlot[] capturedSlots, FrameSlot[] capturingSlots, FrameDescriptor descriptor, boolean isDef, Expression body) {
		return new Method(name, argumentSlots, capturedSlots, capturingSlots, descriptor, isDef, body);
	}
}
