package orc.error.compiletime;

import orc.error.Locatable;
import orc.error.OrcException;
import orc.error.SourceLocation;


/**
 * 
 * Exceptions generated during Orc compilation from source to
 * portable compiled representations.
 * 
 * @author dkitchin
 *
 */
public class CompilationException extends OrcException implements Locatable {
	protected SourceLocation location;

	public CompilationException(String message) {
		super(message);
	}

	public CompilationException(String message, Throwable cause) {
		super(message, cause);
	}

	public void setSourceLocation(SourceLocation location) {
		this.location = location;
	}

	public SourceLocation getSourceLocation() {
		return location;
	}
	
	public String getMessage() {
		if (location != null) {
			return "At " + location + ": " + super.getMessage();
		} else {
			return super.getMessage();
		}
	}
}
