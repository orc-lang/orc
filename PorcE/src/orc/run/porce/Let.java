package orc.run.porce;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

public class Let extends Expression {
	private final FrameSlot slot;
	@Child
	protected Expression value;
	@Child
	protected Expression body;

	public Let(FrameSlot slot, Expression value, Expression body) {
		this.slot = slot;
		this.value = value;
		this.body = body;
	}

	public Object execute(VirtualFrame frame) {
		frame.setObject(slot, value.execute(frame));
		return body.execute(frame);
	}

	public static Let create(FrameSlot slot, Expression value, Expression body) {
		return new Let(slot, value, body);
	}
}
