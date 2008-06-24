package orc.orchard;

import java.util.Date;

import orc.error.SourceLocation;
import orc.error.TokenException;

public class TokenErrorEvent extends JobEvent {
	public String message;
	public SourceLocation location;
	public TokenErrorEvent() {}
	public TokenErrorEvent(TokenException problem) {
		location = problem.getSourceLocation();
		message = problem.getMessage();
	}
}
