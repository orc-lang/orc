package orc.error.runtime;







/**
 * 
 * Any exception occurring in a well-formed, well-typed
 * site call. These are semantic exceptions from within
 * the site computation itself. 
 * 
 * @author dkitchin
 *
 */

public class SiteException extends TokenException {

	public SiteException(String message) {
		super(message);
	}

	public SiteException(String message, Throwable cause) {
		super(message, cause);
	}

}
