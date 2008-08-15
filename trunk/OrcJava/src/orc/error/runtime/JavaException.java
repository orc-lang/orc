package orc.error.runtime;






/**
 * 
 * A container for Java-level exceptions raised by code
 * implementing sites. These are wrapped as Orc exceptions
 * to localize the failure to the calling token.
 * 
 * @author dkitchin
 *
 */
public class JavaException extends SiteException {
	public JavaException(Throwable cause) {
		super(cause.toString(), cause);
	}
	public String toString() {
		return getCause().toString();
	}
}
