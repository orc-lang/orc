package orc.run.porce;

import com.oracle.truffle.api.frame.VirtualFrame;

public class TryFinally extends Expression {
	@Child
	protected Expression body;
	@Child
	protected Expression handler;

	public TryFinally(Expression body, Expression handler) {
		this.body = body;
		this.handler = handler;
	}

	public void executePorcEUnit(VirtualFrame frame) {
		try {
			body.executePorcEUnit(frame);
		} finally {
			handler.executePorcEUnit(frame);
		}
	}

	public static TryFinally create(Expression body, Expression handler) {
		return new TryFinally(body, handler);
	}
}
