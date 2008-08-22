package orc.trace.events;

/**
 * Leaving the left side of a semicolon combinator.
 * 
 * @author quark
 */
public class BeforeEvent extends Event {
	@Override
	public String getType() {
		return "before";
	}
}
