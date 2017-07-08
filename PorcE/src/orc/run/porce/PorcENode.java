package orc.run.porce;

import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

@NodeInfo(language = "PorcE")
@TypeSystemReference(PorcETypes.class)
public abstract class PorcENode extends Node {
	public abstract Object execute(VirtualFrame frame);

	public void executePorcEUnit(VirtualFrame frame) {
		execute(frame);
	}

	public PorcEContinuationClosure executePorcEContinuationClosure(VirtualFrame frame)
			throws UnexpectedResultException {
		return PorcETypesGen.expectPorcEContinuationClosure(execute(frame));
	}
}
