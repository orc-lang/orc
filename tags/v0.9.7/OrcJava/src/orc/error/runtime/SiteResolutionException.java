package orc.error.runtime;

/**
 * Indicate a problem with site resolution. Ideally
 * this would be a loadtime error, but currently site
 * resolution is done at runtime.
 * @author quark
 */
public class SiteResolutionException extends TokenException {
	public SiteResolutionException(String message, Throwable cause) {
		super(message, cause);
	}

	public SiteResolutionException(String message) {
		super(message);
	}
}
