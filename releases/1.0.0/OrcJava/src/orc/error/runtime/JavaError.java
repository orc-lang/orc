package orc.error.runtime;

import orc.runtime.Token;






/**
 * A container for Java-level exceptions raised while
 * processing a token. These are wrapped as Orc exceptions
 * so they can be handled by {@link Token#error(TokenException)}.
 */
public class JavaError extends TokenError {
	public JavaError(Throwable cause) {
		super(cause.toString(), cause);
	}
	public String toString() {
		return getCause().toString();
	}
}
