package orc.error.runtime;

public class StackLimitReachedError extends TokenException {
	public StackLimitReachedError() {
		super("Stack limit reached");
	}
}
