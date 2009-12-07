package orc.error.runtime;

public class StackLimitReachedError extends TokenException {
	public StackLimitReachedError(int limit) {
		super("Stack limit (limit=" + limit + ") reached");
	}
}
