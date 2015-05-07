package orc.error.runtime;

public class CapabilityException extends TokenException {
	public CapabilityException(String name) {
		super("This engine does not have the capability '" + name + "'");
	}
}
