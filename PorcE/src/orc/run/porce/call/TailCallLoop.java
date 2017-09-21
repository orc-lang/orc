
package orc.run.porce.call;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RepeatingNode;
import com.oracle.truffle.api.profiles.BranchProfile;

import orc.run.porce.NodeBase;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecutionRef;
import orc.run.porce.runtime.TailCallException;

public class TailCallLoop extends NodeBase {
	@Child
    protected volatile LoopNode loop;
	
	protected final PorcEExecutionRef execution; 
	
	// TODO: There are two tailCallProfile (here and in the nested class). This may be a good thing, but it's probably just duplication we don't need.
	private final BranchProfile tailCallProfile = BranchProfile.create();

    protected TailCallLoop(final PorcEExecutionRef execution) {
		this.execution = execution;
    }
    
    private LoopNode getLoopNode(VirtualFrame frame) {
    	// FIXME: Find all other instances of double checked locking and make sure they are all correct. Maybe abstract this into utility of some kind.
    	if (loop == null) {
    		CompilerDirectives.transferToInterpreterAndInvalidate();
    		// Double checked locking is valid only because we require JRE > 5 and that loop is volatile.
    		atomic(() -> {
    	    	if (loop == null) {
    	    		loop = insert(Truffle.getRuntime().createLoopNode(new CatchTailCallRepeatingNode(frame.getFrameDescriptor())));
    	    	}
    		});
    	}
    	return loop;
    }

	public void executeTailCalls(VirtualFrame frame, TailCallException e) {
		tailCallProfile.enter();
		LoopNode loop = getLoopNode(frame);
		CatchTailCallRepeatingNode repeating = (CatchTailCallRepeatingNode) loop.getRepeatingNode();
		repeating.setNextCall(frame, e.target, e.arguments);
		loop.executeLoop(frame);
	}
	
	public static TailCallLoop create(final PorcEExecutionRef execution) {
		return new TailCallLoop(execution);
	}
    
    protected class CatchTailCallRepeatingNode extends Node implements RepeatingNode {
		private final BranchProfile tailCallProfile = BranchProfile.create();
		private final BranchProfile returnProfile = BranchProfile.create();
		private final FrameSlot targetSlot;
		private final FrameSlot argumentsSlot;
		
		@Child
		protected InternalCPSDispatch call = InternalCPSDispatch.createBare(execution); 

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
				Object target = getTarget(frame);
				Object[] arguments = getArguments(frame);
			    /*Logger.entering(() -> getClass().getName(), () -> "executeRepeating", 
			    		() -> scala.collection.JavaConversions.asScalaBuffer(Arrays.asList(target, arguments)).seq());
			    		*/
				call.executeDispatch(frame, target, arguments);
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
