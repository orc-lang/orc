package orc.run.porce;

import static com.oracle.truffle.api.CompilerDirectives.SLOWPATH_PROBABILITY;
import static com.oracle.truffle.api.CompilerDirectives.injectBranchProbability;

import com.oracle.truffle.api.CompilerDirectives.*;
import static com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.RootNode;

import java.util.logging.Level;

import orc.ast.porc.PorcAST;
import orc.error.runtime.ArityMismatchException;
import orc.error.runtime.HaltException;
import orc.run.porce.runtime.KilledException;
import scala.Option;

public class PorcERootNode extends RootNode implements HasPorcNode {
	private Option<PorcAST> porcNode = Option.apply(null);
	
	public void setPorcAST(PorcAST ast) {
		porcNode = Option.apply(ast);
	}
	
	public Option<PorcAST> porcNode() {
		return porcNode;
	}
	
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
		
		// FIXME: PERFORMANCE: Evaluate using specialized Variable nodes which know which kind of value they are loading (mainly 
		//    closed, but also argument) and load the value directly to avoid these transfers here. The captured look-up could even
		//    profile and specialize on the specific captured variable array. This would eliminate all of these loops.

		// Load all the arguments into the frame.
		for (int i = 0; i < argumentSlots.length; i++) {
			frame.setObject(argumentSlots[i], arguments[i+1]);
		}
		
		// Load all the captured variables into the frame.
		for (int i = 0; i < capturedSlots.length; i++) {
			frame.setObject(capturedSlots[i], captureds[i]);
		}
		
		try {
			Object ret = body.execute(frame);
			return ret;
		} catch(KilledException | HaltException e) {
			transferToInterpreter();
			Logger.log(Level.WARNING, () -> "Caught " + e + " from root node.", e);
			return PorcEUnit.SINGLETON;
		}
	}
	
	@TruffleBoundary(allowInlining = true)
	private static void throwArityException(int nReceived, int nExpected) {
		throw new ArityMismatchException(nExpected, nReceived);
	}

}
