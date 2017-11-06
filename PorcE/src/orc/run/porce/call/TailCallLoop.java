package orc.run.porce.call;

import java.util.concurrent.locks.Lock;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RepeatingNode;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;

import orc.run.porce.Logger;
import orc.run.porce.NodeBase;
import orc.run.porce.PorcERootNode;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecutionRef;
import orc.run.porce.runtime.TailCallException;

public class TailCallLoop extends NodeBase {
	@Child
    protected volatile LoopNode loop = null;
	
	protected final PorcEExecutionRef execution; 
	
    protected TailCallLoop(final PorcEExecutionRef execution) {
		this.execution = execution;
    }
    
    private LoopNode getLoopNode(VirtualFrame frame) {
    	if (loop == null) {
    		CompilerDirectives.transferToInterpreterAndInvalidate();
	    	computeAtomicallyIfNull(() -> loop, (v) -> loop = v, () ->
	    		insert(Truffle.getRuntime().createLoopNode(new CatchTailCallRepeatingNode(frame.getFrameDescriptor(), execution))));
    	}
    	return loop;
    }

	public void executeTailCalls(VirtualFrame frame, TailCallException e) {
		LoopNode loop = getLoopNode(frame);
		CatchTailCallRepeatingNode repeating = (CatchTailCallRepeatingNode) loop.getRepeatingNode();
		repeating.initFrame(frame);
		repeating.setNextCall(frame, e);
		loop.executeLoop(frame);
	}

	public final void addSurroundingFunction(VirtualFrame frame, Object target) {
	}

	public static TailCallLoop create(final PorcEExecutionRef execution) {
		return new TailCallLoop(execution);
	}
	
	
	
    protected abstract static class TailCallNode extends NodeBase {
    	protected final PorcEExecutionRef execution;

    	protected TailCallNode(PorcEExecutionRef execution) {
    		this.execution = execution;
    	}
    	
    	//public abstract boolean executeDispatch(VirtualFrame frame, Object target, Object[] arguments, boolean isFirstCallInChain);
    }
    
    protected final static class TailCallSpecializationNode extends TailCallNode {
    	@Child
    	protected DirectCallNode call;
    	@Child
    	protected TailCallNode next;
    	
    	protected final RootCallTarget target;
    	protected final ConditionProfile firstCallProfile = ConditionProfile.createCountingProfile();
    	protected final ConditionProfile nextCallProfile = ConditionProfile.createCountingProfile();
    	protected final ConditionProfile tceReuseProfile = ConditionProfile.createBinaryProfile();
    	
    	protected TailCallSpecializationNode(RootCallTarget target, TailCallNode next, PorcEExecutionRef execution) {
    		super(execution);
    		this.target = target;
    		this.call = DirectCallNode.create(target);
    		this.next = next;
		}

    	protected boolean matchesSpecific(RootCallTarget target) {
    		return this.target == target;
    	}
    	
    	public static TailCallNode create(final RootCallTarget target, final TailCallNode next, final PorcEExecutionRef execution) {
			return new TailCallSpecializationNode(target, next, execution);
    	}
    }
    
    protected final static class TailCallTerminalNode extends TailCallNode {
    	protected TailCallTerminalNode(PorcEExecutionRef execution) {
    		super(execution);
		}

    	public static TailCallNode create(final PorcEExecutionRef execution) {
			return new TailCallTerminalNode(execution);
    	}
    }
    
    protected static final class CatchTailCallRepeatingNode extends Node implements RepeatingNode {
    	private final PorcEExecutionRef execution;
    	
		private final BranchProfile tailCallProfile = BranchProfile.create();
		private final BranchProfile returnProfile = BranchProfile.create();
		private final ConditionProfile loopTCEReuseProfile = ConditionProfile.createBinaryProfile();
		
		private final FrameSlot tceSlot;
		
		@Child
		protected TailCallNode rootCall;

		public CatchTailCallRepeatingNode(FrameDescriptor frameDescriptor, PorcEExecutionRef execution) {
			this.execution = execution;
			this.tceSlot = frameDescriptor.findOrAddFrameSlot("<tailCallException>", FrameSlotKind.Object);
			this.rootCall = TailCallTerminalNode.create(execution);
		}
		
		public void initFrame(VirtualFrame frame) {
			frame.setObject(tceSlot, TailCallException.create(null));
		}
		
		public TailCallException getTCE(VirtualFrame frame) {
			TailCallException tce = (TailCallException) FrameUtil.getObjectSafe(frame, tceSlot);
			return tce;
		}
		
		public void setNextCall(VirtualFrame frame, ConditionProfile TCEReusedProfile, TailCallException e) {
			if (TCEReusedProfile.profile(e != getTCE(frame)))
				getTCE(frame).set(e.getTarget(), e.getArguments());
		}
		
		public void setNextCall(VirtualFrame frame, TailCallException e) {
			if (e != getTCE(frame))
				getTCE(frame).set(e.getTarget(), e.getArguments());
		}

		protected TailCallException throwTCE(final VirtualFrame frame) {
			TailCallException tce = getTCE(frame);
			throw tce;
		}

		@Override
		public boolean executeRepeating(VirtualFrame frame) {
			try {
				invokeCalls(rootCall, frame);
				returnProfile.enter();
				return false;
			} catch (TailCallException e) {
				tailCallProfile.enter();
				assert e == getTCE(frame);
				//setNextCall(frame, loopTCEReuseProfile, e);
				return true;
			}
		}

		@ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.FULL_EXPLODE_UNTIL_RETURN)
		protected boolean alreadyContains(final RootCallTarget target) {
			TailCallNode call = rootCall;
			while(true) {
				if (call instanceof TailCallSpecializationNode) {
					TailCallSpecializationNode spec = (TailCallSpecializationNode) call;
					if (spec.matchesSpecific(target)) {
						return true;
					} else {
						call = spec.next;
						continue;
					}
				} else {
					return false;
				}
			}
		}
		
		protected TailCallSpecializationNode extendAfter(TailCallNode call, final RootCallTarget target) {
			Lock lock = getLock();
			lock.lock();
			try {
				TailCallTerminalNode term = (TailCallTerminalNode) call;
				//Logger.info(() -> "Extending: " + target.toString() + " " + target.hashCode());
				return (TailCallSpecializationNode) term.replace(
	    				TailCallSpecializationNode.create(target, TailCallTerminalNode.create(execution), execution), 
	    				"Extend TailCall chain: " + target.toString() + " " + target.hashCode());
			} finally {
				lock.unlock();
			}
		}


		@ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.FULL_EXPLODE_UNTIL_RETURN)
		private void invokeCalls(TailCallNode call, final VirtualFrame frame) {
			final RootCallTarget target = getTCE(frame).getTarget();
			//final Object[] arguments = getTCE(frame).getArguments();
			while(true) {
				if (call instanceof TailCallSpecializationNode) {
					TailCallSpecializationNode spec = (TailCallSpecializationNode) call;
					if (spec.firstCallProfile.profile(spec.matchesSpecific(target))) {
						invokeAdditionalCalls(call, frame);
						break;
					} else {
						call = spec.next;
						continue;
					}
				} else {
		    		CompilerDirectives.transferToInterpreterAndInvalidate();
					assert call instanceof TailCallTerminalNode;
		    		extendCalls(extendAfter(call, target), frame);
		    		break;
				}
			}
		}

		private void extendCalls(TailCallSpecializationNode spec, final VirtualFrame frame) {
			CompilerAsserts.neverPartOfCompilation();
			while(true) {
				try {
					final Object[] arguments = getTCE(frame).getArguments();
					spec.call.call(arguments);
					break;
				} catch (TailCallException e) {
					setNextCall(frame, e);
					//assert e == getTCE(frame);
					
					final RootCallTarget target = getTCE(frame).getTarget();
					
					if (alreadyContains(target)) {
						throwTCE(frame);
						break;
					}
					spec = extendAfter(spec.next, target);
				}
			}
		}
		
		private void invokeAdditionalCalls(final TailCallNode call, final VirtualFrame frame) {
			final RootCallTarget target = getTCE(frame).getTarget();
			final Object[] arguments = getTCE(frame).getArguments();
			if (call instanceof TailCallSpecializationNode) {
				TailCallSpecializationNode spec = (TailCallSpecializationNode) call;
				if (spec.nextCallProfile.profile(spec.matchesSpecific(target))) {
					try {
						spec.call.call(arguments);
					} catch (TailCallException e) {
						//assert e == getTCE(frame);
						setNextCall(frame, spec.tceReuseProfile, e);
						invokeAdditionalCalls(spec.next, frame);
					}
				} else {
					throwTCE(frame);
				}
			} else {
				throwTCE(frame);
			}
		}

		/*
		@ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.FULL_UNROLL)
		private void invokeCalls(TailCallNode call, final VirtualFrame frame, RootCallTarget target, Object[] arguments) {
			boolean isFirstCall = true;
			// getRootNode().getCurrentContext(orc.run.porce.PorcELanguage.class);
			while(true) {
				if (call instanceof TailCallSpecializationNode) {
					TailCallSpecializationNode spec = (TailCallSpecializationNode) call;
					if ((spec.next instanceof TailCallTerminalNode && spec.matchesSpecific(target)) ||
							spec.thisOrOtherProfile.profile(spec.matchesSpecific(target))) {
						try {
							isFirstCall = false;
							spec.call.call(arguments);
							break;
						} catch (TailCallException e) {
							target = e.target;
							arguments = e.arguments;
							call = spec.next;
							continue;
						}
					} else {
						call = spec.next;
						continue;
					}
				} else {
					assert call instanceof TailCallTerminalNode;
					if (isFirstCall) {
			    		CompilerDirectives.transferToInterpreterAndInvalidate();
			    		Lock lock = getLock();
			    		lock.lock();
			    		try {
							TailCallTerminalNode term = (TailCallTerminalNode) call;
				    		TailCallNode n = term.replace(
				    				TailCallSpecializationNode.create(target, TailCallTerminalNode.create(execution), execution), 
				    				"Extend TailCall chain");
				    		//final Object t = target;
				    		//Logger.info(() -> "Extending " + getRootNode().getName() + " with " + t);
				    		call = n;
				    		continue;
			    		} finally {
			    			lock.unlock();
			    		}
					}
					TailCallException tce = getTCE(frame);
					tce.target = target;
					tce.arguments = arguments;
		    		throw tce;
				}
			}
		}
			*/
		
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
