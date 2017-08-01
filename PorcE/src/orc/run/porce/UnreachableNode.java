package orc.run.porce;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;

public class UnreachableNode extends Expression {
	@Override
	public Object execute(VirtualFrame frame) {
		CompilerDirectives.transferToInterpreter();
		throw new AssertionError("This node should never be reached.");
	}
}
