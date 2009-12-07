package orc.error.compiletime;




/**
 * Problem parsing the text of an Orc program. Mostly this
 * is a wrapper around the exceptions thrown by whatever
 * parsing library we use.
 * 
 * @author quark
 */
public class ParsingException extends CompilationException {
	public ParsingException(String message, Throwable cause) {
		super(message, cause);
	}
	public ParsingException(String message) {
		super(message);
	}
}
