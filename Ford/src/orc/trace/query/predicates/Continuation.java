package orc.trace.query.predicates;

/**
 * Thunk which can be evaluated to get a query result. Think of it like a
 * predicate already combined with a frame. Since a result may have a failure
 * continuation, a continuation is like a stream; evaluating it returns null
 * (end of stream) or a result (cons of frame and the tail of the stream).
 */
public interface Continuation {
	public Result evaluate();
}
