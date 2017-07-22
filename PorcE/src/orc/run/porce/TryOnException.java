package orc.run.porce;

import com.oracle.truffle.api.frame.VirtualFrame;

import orc.error.runtime.HaltException;
import orc.run.porce.runtime.KilledException;

public class TryOnException extends Expression {
	@Child
	protected Expression body;
	@Child
	protected Expression handler;

	public TryOnException(Expression body, Expression handler) {
		this.body = body;
		this.handler = handler;
	}

	public void executePorcEUnit(VirtualFrame frame) {
		try {
			body.executePorcEUnit(frame);
		} catch(HaltException | KilledException e) {
			handler.executePorcEUnit(frame);
		}
	}

	public static TryOnException create(Expression body, Expression handler) {
		return new TryOnException(body, handler);
	}
}
