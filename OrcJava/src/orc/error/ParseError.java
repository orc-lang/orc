package orc.error;

/**
 * Problem parsing the text of an Orc program. Mostly this
 * is a wrapper around the exceptions thrown by whatever
 * parsing library we use.
 * 
 * @author quark
 */
public class ParseError extends CompilationException {
	public ParseError(String message, Throwable cause) {
		super(message, cause);
	}
	public ParseError(String message) {
		super(message);
	}
}
