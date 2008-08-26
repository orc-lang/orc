package orc.trace.query.predicates;

import orc.trace.query.Frame;

/**
 * A result from a predicate consists of a frame
 * with bound variables and a continuation to
 * invoke for alternative results (for backtracking).
 * null is used to indicate a failure result.
 */
public final class Result {
	public static final Result NO = null;
	private final Frame frame;
	private final Continuation failure;
	
	public Result(Frame frame, Continuation failure) {
		this.frame = frame;
		this.failure = failure;
	}
	
	/**
	 * Construct a result with no alternative.
	 */
	public Result(Frame frame) {
		this(frame, new Continuation() {
			public Result evaluate() {
				return Result.NO;
			}
		});
	}
	
	/**
	 * @return the next alternative
	 */
	public Result next() {
		return failure.evaluate();
	}
	
	/**
	 * @return the frame with the variable bindings
	 */
	public Frame getFrame() {
		return frame;
	}
	
	/**
	 * @return the failure continuation
	 */
	public Continuation getFailure() {
		return failure;
	}
}
