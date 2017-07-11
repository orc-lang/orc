package orc.run.porce;

import com.oracle.truffle.api.CompilerDirectives.*;
import static com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import orc.error.runtime.ArityMismatchException;

public class PorcERootNode extends RootNode {
	protected @Child Expression body;
	private final FrameSlot[] argumentSlots;
	private final FrameSlot[] capturedSlots;

	public PorcERootNode(FrameSlot[] argumentSlots, FrameSlot[] capturedSlots, FrameDescriptor descriptor, Expression body) {
		super(null, descriptor);
		this.argumentSlots = argumentSlots;
		this.capturedSlots = capturedSlots;
		this.body = body;
	}

	@Override
	@ExplodeLoop
	public Object execute(VirtualFrame frame) {
		Object[] arguments = frame.getArguments();
		if(injectBranchProbability(SLOWPATH_PROBABILITY, 
				arguments.length != argumentSlots.length+1)) {
			throwArityException(arguments.length-1, argumentSlots.length);
		}
		Object[] captureds = (Object[]) arguments[0];
		if(injectBranchProbability(SLOWPATH_PROBABILITY,
				captureds.length != capturedSlots.length)) {
			InternalPorcEError.capturedLengthError(capturedSlots.length, captureds.length);
		}
		
		// TODO: PERFORMANCE: Evaluate using specialized Variable nodes which know which kind of value they are loading (mainly 
		//    closed, but also argument) and load the value directly to avoid these transfers here. The captured look-up could even
		//    profile and specialize on the specific captured variable array. 
		//    This could use an assumption to invalidate the specializations if the same function is instantiated again with a 
		//    different context. This would require a good way to check that the contexts were the same since multiple 
		//    instantiations of the same function may end up with the same context even if their declarations where running
		//    in different contexts.

		// Load all the arguments into the frame.
		for (int i = 0; i < argumentSlots.length; i++) {
			frame.setObject(argumentSlots[i], arguments[i+1]);
		}
		// Load all the captured variables into the frame.
		for (int i = 0; i < capturedSlots.length; i++) {
			frame.setObject(capturedSlots[i], captureds[i]);
		}
		Object ret = body.execute(frame);
		return ret;
	}
	
	@TruffleBoundary(allowInlining = true)
	private static void throwArityException(int nReceived, int nExpected) {
		throw new ArityMismatchException(nExpected, nReceived);
	}

}
