package orc.orchard.events;

import orc.error.SourceLocation;
import orc.error.runtime.TokenException;

public class TokenErrorEvent extends JobEvent {
	public String message;
	public SourceLocation location;
	public TokenErrorEvent() {}
	public TokenErrorEvent(TokenException problem) {
		location = problem.getSourceLocation();
		message = problem.getMessage();
	}
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
