package orc.trace.query.predicates;

import orc.trace.query.Frame;


public final class Result {
	public static final Result NO = null;
	public final Frame frame;
	public final Continuation failure;
	public Result(Frame frame, Continuation failure) {
		this.frame = frame;
		this.failure = failure;
	}
	public Result(Frame frame) {
		this(frame, new Continuation() {
			public Result evaluate() {
				return Result.NO;
			}
		});
	}
	
	public Result next() {
		return failure.evaluate();
	}
	
	public String toString() {
		return frame.toString();
	}
}
