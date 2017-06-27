package orc.run.porce;

import com.oracle.truffle.api.frame.VirtualFrame;

import orc.run.porce.runtime.KilledException;

public class TryOnKilled extends Expression {
	@Child
	protected Expression body;
	@Child
	protected Expression handler;

	public TryOnKilled(Expression body, Expression handler) {
		this.body = body;
		this.handler = handler;
	}

	public void executePorcEUnit(VirtualFrame frame) {
		try {
			body.executePorcEUnit(frame);
		} catch(KilledException e) {
			handler.executePorcEUnit(frame);
		}
	}

	public static TryOnKilled create(Expression body, Expression handler) {
		return new TryOnKilled(body, handler);
	}
}
