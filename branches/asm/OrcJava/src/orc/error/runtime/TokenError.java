package orc.error.runtime;

import orc.error.Locatable;
import orc.error.OrcError;
import orc.error.SourceLocation;

/**
 * A non-recoverable error at a token, which must result in halting the whole
 * engine. This extends TokenException because it simplifies error handling to
 * have a common superclass for token errors and exceptions.
 * 
 * @quark
 */
public abstract class TokenError extends TokenException {

	public TokenError(String message) {
		super(message);
	}
	
	public TokenError(String message, Throwable cause) {
		super(message, cause);
	}
}
