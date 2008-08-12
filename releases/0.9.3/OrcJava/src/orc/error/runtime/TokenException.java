package orc.error.runtime;

import orc.error.Locatable;
import orc.error.SourceLocation;

/**
 * 
 * A localized failure at runtime. Errors of this type cause the executing
 * token to remain silent and leave the execution, but they do not otherwise
 * disrupt the execution. Since tokens are located at specific nodes and
 * can thus be assigned a source location, a TokenException implements
 * Locatable and will typically have its source location set before the
 * exception is passed back to the engine.
 * 
 * @author dkitchin
 *
 */
public abstract class TokenException extends ExecutionException implements Locatable {

	SourceLocation loc = SourceLocation.UNKNOWN;
	
	public TokenException(String message) {
		super(message);
	}
	
	public TokenException(String message, Throwable cause) {
		super(message, cause);
	}

	public SourceLocation getSourceLocation() {
		return loc;
	}

	public void setSourceLocation(SourceLocation location) {
		this.loc = location;
	}

}
