
package orc.run.porce.call;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RepeatingNode;
import com.oracle.truffle.api.profiles.BranchProfile;

import orc.run.porce.Expression;
import orc.run.porce.PorcEUnit;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecutionRef;
import orc.run.porce.runtime.TailCallException;

public class CatchTailCall extends Expression {
	@Child
    protected LoopNode loop;
	@Child
	protected Expression body;
	
	protected final PorcEExecutionRef execution;

	@Override
    public void setTail(boolean v) {
		super.setTail(v);
		body.setTail(v);
	}

    protected CatchTailCall(final Expression body, final PorcEExecutionRef execution) {
    	this.body = body;
		this.execution = execution;
    }
    
    private LoopNode getLoopNode(VirtualFrame frame) {
    	if (loop == null) {
    		CompilerDirectives.transferToInterpreterAndInvalidate();
    		loop = insert(Truffle.getRuntime().createLoopNode(new CatchTailCallRepeatingNode(frame.getFrameDescriptor())));
    	}
    	return loop;
    }
	
    @Specialization
    public PorcEUnit run(VirtualFrame frame) {
    	try {
    		body.executePorcEUnit(frame);
			return PorcEUnit.SINGLETON;
    	} catch (TailCallException e) {
    		LoopNode loop = getLoopNode(frame);
    		CatchTailCallRepeatingNode repeating = (CatchTailCallRepeatingNode) loop.getRepeatingNode();
    		repeating.setNextCall(frame, e.target, e.arguments);
    		loop.executeLoop(frame);
			return PorcEUnit.SINGLETON;
    	}
    }

    public static CatchTailCall create(final Expression body, final PorcEExecutionRef execution) {
        return CatchTailCallNodeGen.create(body, execution);
    }
    
    protected class CatchTailCallRepeatingNode extends Node implements RepeatingNode {
		private final BranchProfile tailCallProfile = BranchProfile.create();
		private final BranchProfile returnProfile = BranchProfile.create();
		private final FrameSlot targetSlot;
		private final FrameSlot argumentsSlot;
		
		@Child
		protected InternalCPSDispatch call = InternalCPSDispatch.create(execution); 

		public CatchTailCallRepeatingNode(FrameDescriptor frameDescriptor) {
			this.targetSlot = frameDescriptor.findOrAddFrameSlot("<OSRtailCallTarget>", FrameSlotKind.Object);
			this.argumentsSlot = frameDescriptor.findOrAddFrameSlot("<OSRtailCallArguments>", FrameSlotKind.Object);
		}
		
		public void setNextCall(VirtualFrame frame, PorcEClosure target, Object[] arguments) {
			frame.setObject(targetSlot, target);
			frame.setObject(argumentsSlot, arguments);
		}

		private Object getTarget(VirtualFrame frame) {
			Object target = FrameUtil.getObjectSafe(frame, targetSlot);
			frame.setObject(targetSlot, null); // Clear it to avoid leaking during the call
			return target;
		}

		private Object[] getArguments(VirtualFrame frame) {
			Object[] arguments = (Object[]) FrameUtil.getObjectSafe(frame, argumentsSlot);
			frame.setObject(argumentsSlot, null); // Clear it to avoid leaking during the call
			return arguments;
		}

		@Override
		public boolean executeRepeating(VirtualFrame frame) {
			try {
				call.executeDispatch(frame, getTarget(frame), getArguments(frame));
				returnProfile.enter();
				return false;
			} catch (TailCallException e) {
				tailCallProfile.enter();
	    		setNextCall(frame, e.target, e.arguments);
				return true;
			}
		}

	}

}
