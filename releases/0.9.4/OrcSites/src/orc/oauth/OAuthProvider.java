package orc.oauth;

import java.io.IOException;
import java.security.PrivateKey;
import java.util.List;

import kilim.Pausable;
import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthException;
import net.oauth.signature.RSA_SHA1;

/**
 * Abstract out the details of OAuth authentication so
 * that sites can use it without relying on a particular
 * platform (UI or web).
 * 
 * @author quark
 */
public abstract class OAuthProvider {
	protected SimpleOAuth oauth;
	public OAuthProvider(String properties) throws IOException {
		oauth = new SimpleOAuth(properties);
	}
	
	public OAuthAccessor authenticate(String name) throws Pausable, Exception {
		return authenticate(name, OAuth.newList());
	}
	
	/**
	 * Get an authenticated OAuthAccessor.
	 * @param  
	 */
	public OAuthAccessor authenticate(String name, List<OAuth.Parameter> request)
	throws Pausable, Exception {
		throw new AssertionError("Must override OAuthProvider#authenticate(String)");
	}
	
	public final OAuthConsumer getConsumer(String name) throws OAuthException {
		return oauth.getConsumer(name);
	}
	
	public final PrivateKey getPrivateKey(String consumer) throws OAuthException {
		return (PrivateKey)oauth.getConsumer(consumer)
						.getProperty(RSA_SHA1.PRIVATE_KEY);
	}
}
