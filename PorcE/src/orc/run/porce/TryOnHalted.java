package orc.run.porce;

import com.oracle.truffle.api.frame.VirtualFrame;

import orc.error.runtime.HaltException;

public class TryOnHalted extends Expression {
	@Child
	protected Expression body;
	@Child
	protected Expression handler;

	public TryOnHalted(Expression body, Expression handler) {
		this.body = body;
		this.handler = handler;
	}

	public void executePorcEUnit(VirtualFrame frame) {
		try {
			body.executePorcEUnit(frame);
		} catch(HaltException e) {
			handler.executePorcEUnit(frame);
		}
	}

	public static TryOnHalted create(Expression body, Expression handler) {
		return new TryOnHalted(body, handler);
	}
}
