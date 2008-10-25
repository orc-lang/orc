package orc.error.runtime;

public class StackLimitReachedException extends TokenException {
	public StackLimitReachedException() {
		super("Stack limit reached");
	}
}
