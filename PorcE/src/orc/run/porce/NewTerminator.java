package orc.run.porce;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import orc.run.porce.runtime.TerminatorNested;
import orc.run.porce.runtime.Terminator;

public class NewTerminator extends Expression {
	@Child
	protected Expression parent;

	public NewTerminator(Expression parent) {
		this.parent = parent;
	}

	public Object execute(VirtualFrame frame) {
		return executeTerminator(frame);
	}

	public Terminator executeTerminator(VirtualFrame frame) {
		try {
			return new TerminatorNested(parent.executeTerminator(frame));
		} catch (UnexpectedResultException e) {
			throw new Error(e);
		}
	}

	public static NewTerminator create(Expression parent) {
		return new NewTerminator(parent);
	}
}
