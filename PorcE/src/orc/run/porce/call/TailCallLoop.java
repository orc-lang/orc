
package orc.run.porce.call;

import java.util.HashSet;

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
import orc.run.porce.PorcERootNode;
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
    	if (loop == null) {
    		CompilerDirectives.transferToInterpreterAndInvalidate();
	    	computeAtomicallyIfNull(() -> loop, (v) -> loop = v, () ->
	    		insert(Truffle.getRuntime().createLoopNode(new CatchTailCallRepeatingNode(frame.getFrameDescriptor()))));
    	}
    	return loop;
    }

	public void executeTailCalls(VirtualFrame frame, TailCallException e) {
		tailCallProfile.enter();
		LoopNode loop = getLoopNode(frame);
		CatchTailCallRepeatingNode repeating = (CatchTailCallRepeatingNode) loop.getRepeatingNode();
		repeating.setNextCall(frame, e.target, e.arguments);
		loop.executeLoop(frame);
		repeating.finalizeFrame(frame);
	}

	public void addSurroundingFunction(VirtualFrame frame, Object target) {
		if (CompilerDirectives.inInterpreter()) {
			CatchTailCallRepeatingNode repNode = (CatchTailCallRepeatingNode) getLoopNode(frame).getRepeatingNode();
			repNode.addSurroundingFunction(frame, target);
		}
	}

	public static TailCallLoop create(final PorcEExecutionRef execution) {
		return new TailCallLoop(execution);
	}
    
    protected final class CatchTailCallRepeatingNode extends Node implements RepeatingNode {
		private final BranchProfile tailCallProfile = BranchProfile.create();
		private final BranchProfile returnProfile = BranchProfile.create();
		private final FrameSlot targetSlot;
		private final FrameSlot argumentsSlot;
		private final FrameSlot surroundingFunctionsSlot; 
		
		@Child
		protected InternalCPSDispatch call = InternalCPSDispatch.createBare(execution);

		public CatchTailCallRepeatingNode(FrameDescriptor frameDescriptor) {
			this.surroundingFunctionsSlot = frameDescriptor.findOrAddFrameSlot("<surroundingFunctions>", FrameSlotKind.Object);
			this.targetSlot = frameDescriptor.findOrAddFrameSlot("<OSRtailCallTarget>", FrameSlotKind.Object);
			this.argumentsSlot = frameDescriptor.findOrAddFrameSlot("<OSRtailCallArguments>", FrameSlotKind.Object);
		}
		
		public void finalizeFrame(VirtualFrame frame) {
			if (CompilerDirectives.inInterpreter())
				frame.setObject(surroundingFunctionsSlot, null);
		}
		
		public void addSurroundingFunction(VirtualFrame frame, Object target) {
			if (CompilerDirectives.inInterpreter() && 
					target instanceof PorcEClosure &&
					((PorcEClosure)target).body.getRootNode() instanceof PorcERootNode) {
				PorcERootNode root = (PorcERootNode)((PorcEClosure)target).body.getRootNode();
				HashSet<PorcERootNode> a = getSurroundingFunctions(frame);
				a.add(root);
			}
		}

		@SuppressWarnings("unchecked")
		protected HashSet<PorcERootNode> getSurroundingFunctions(VirtualFrame frame) {
			if (CompilerDirectives.inInterpreter()) {
				HashSet<PorcERootNode> s = (HashSet<PorcERootNode>) FrameUtil.getObjectSafe(frame, surroundingFunctionsSlot);
				if (s == null) {
					s = new HashSet<PorcERootNode>();
					frame.setObject(surroundingFunctionsSlot, s);
				}
				return s;
			} else {
				return null;
			}
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
			// TODO: Evaluate in depth if this is good or bad. It showed up on a profile and probably will not 
			//   help much because arguments will still be stored in the frame.
			//frame.setObject(argumentsSlot, null); // Clear it to avoid leaking during the call
			return arguments;
		}

		@Override
		public boolean executeRepeating(VirtualFrame frame) {
	        long startTime = 0;
	        if (CompilerDirectives.inInterpreter())
	        	startTime = System.nanoTime();
			try {
				Object target = getTarget(frame);
				Object[] arguments = getArguments(frame);
			    /*Logger.entering(() -> getClass().getName(), () -> "executeRepeating", 
			    		() -> scala.collection.JavaConversions.asScalaBuffer(Arrays.asList(target, arguments)).seq());
			    		*/
	        	if (CompilerDirectives.inInterpreter()) {
					try {
						call.executeDispatch(frame, target, arguments);
					} finally {
						if (startTime > 0 && getSurroundingFunctions(frame) != null) {
							long t = System.nanoTime() - startTime;
							for (PorcERootNode n : getSurroundingFunctions(frame)) {
								n.addRunTime(t);
							}
				    		addSurroundingFunction(frame, target);
						}
					}
	        	} else {
					call.executeDispatch(frame, target, arguments);
	        	}
				returnProfile.enter();
				return false;
			} catch (TailCallException e) {
				tailCallProfile.enter();
	    		setNextCall(frame, e.target, e.arguments);
				return true;
			}
		}

		@Override
		public String toString() {
			RootNode root = getRootNode();
			if (root instanceof PorcERootNode) {
		        return ((PorcERootNode)root).toString() + "<tailrecloop>";
			} else {
				return "unknown<tailrecloop@" + hashCode() + ">";
			}
		}
	}

}
