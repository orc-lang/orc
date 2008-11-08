package orc.oauth;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.JOptionPane;

import net.oauth.ConsumerProperties;
import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.OAuthProblemException;
import net.oauth.client.HttpClientPool;
import net.oauth.client.OAuthHttpClient;
import net.oauth.signature.RSA_SHA1;

import org.apache.commons.httpclient.HttpClient;

import com.centerkey.utils.BareBonesBrowserLaunch;

/**
 * Wrapper around the OAuth libraries which should simplify using them.
 * It's simple because it gets all the information it needs upfront in
 * a properties file (or object).  See the main method for an example.
 * 
 * <p>All providers are configured with a properties file. A provider identified by NAME
 * must have the following properties defined:
 * 
 * <ul>
 * <li>NAME.serviceProvider.baseURL: prepended to all service URLs
 * <li>NAME.serviceProvider.requestTokenURL: URL to get a request token
 * <li>NAME.serviceProvider.userAuthorizationURL: redirect users to this URL to authorize the request token
 * <li>NAME.serviceProvider.accessTokenURL: URL to exchange request for access token
 * <li>NAME.consumerKey: oauth_consumer_key
 * </ul>
 * 
 * <p>The following properties are optional:
 * <ul>
 * <li>NAME.consumerSecret: oauth_consumer_secret
 * <li>NAME.consumer.oauth_signature_method: oauth_signature_method
 * <li>NAME.consumer.httpMethod: POST or GET
 * </ul>
 * 
 * <p>The following optional properties were added by this library and are not part of the core OAuth library:
 * <ul>
 * <li>NAME.consumer.keystore.storePath: resource path to JKS file containing signing key
 * <li>NAME.consumer.keystore.keyAlias: name of signing key
 * <li>NAME.consumer.keystore.storePassword: password for keystore
 * <li>NAME.consumer.keystore.keyPassword: password for key (if omitted, assumed same as storePassword)
 * </ul>
 * 
 * <p>See {@link ConsumerProperties} for more details on the properties file.
 * 
 * @author quark
 */
public class SimpleOAuth {
	/**
	 * HTTP client factory.
	 */
	private final OAuthHttpClient client = new OAuthHttpClient(
			new HttpClientPool() {
				// This trivial 'pool' simply allocates a new client every time.
				// More efficient implementations are possible.
				public HttpClient getHttpClient(URL server) {
					return new HttpClient();
				}
			});

	/**
	 * Consumer factory.
	 */
	private final ConsumerProperties consumers;

	/**
	 * The names of problems from which a consumer can recover by getting a
	 * fresh token.
	 */
	private static final HashSet<String> RECOVERABLE_PROBLEMS = new HashSet<String>();
	static {
		RECOVERABLE_PROBLEMS.add("token_revoked");
		RECOVERABLE_PROBLEMS.add("token_expired");
		// In the case of permission_unknown, getting a fresh token
		// will cause the Service Provider to ask the User to decide.
		RECOVERABLE_PROBLEMS.add("permission_unknown");
	}

	public SimpleOAuth(String resource) throws IOException {
		URL resourceURL = SimpleOAuth.class.getResource(resource);
		if (resourceURL == null)
			throw new IOException("Could not find resource '"+resource+"'");
		consumers = new ConsumerProperties(
				ConsumerProperties.getProperties(resourceURL));
	}

	public SimpleOAuth(URL resource) throws IOException {
		this(ConsumerProperties.getProperties(resource));
	}

	public SimpleOAuth(Properties properties) {
		consumers = new ConsumerProperties(properties);
	}

	/**
	 * Invoke an OAuth service with custom parameters while handling redirects
	 * transparently.
	 */
	@SuppressWarnings("unchecked")
	private OAuthMessage invoke(OAuthAccessor accessor, String url,
			Collection<? extends Map.Entry> parameters, int maxRedirects)
	throws IOException, OAuthException, URISyntaxException {
		if (maxRedirects < 0) {
			throw new OAuthException("Maximum number of redirects reached");
		}
		try {
			return client.invoke(accessor, url, parameters);
		} catch (OAuthProblemException e) {
			// Check for an HTTP redirect
			switch (e.getHttpStatusCode()) {
			// Usual redirect codes
			case 301:
			case 302:
			case 307:
				break;
			case 303:
				// FIXME: technically we should not re-POST to a 303
				// redirect (or include any of our own query parameters),
				// but I'm not sure that makes any sense with OAuth.
				break;
			default:
				// abort if it is not a redirect
				throw e;
			}
			List<OAuth.Parameter> headers = (List<OAuth.Parameter>)e.getParameters().get(OAuthProblemException.RESPONSE_HEADERS);
			// abort if headers were not found
			if (headers == null) throw e;
			// find the redirect URL
			String redirectUrl = null;
			for (OAuth.Parameter header : headers) {
				if (header.getKey().equals("Location")) {
					redirectUrl = header.getValue();
					break;
				}
			}
			// abort if redirect URL not found
			if (redirectUrl == null) throw e;
			return invoke(accessor, redirectUrl, parameters, maxRedirects - 1);
		}
	}

	public OAuthConsumer getConsumer(String consumer) throws OAuthException {
		OAuthConsumer out;
		try {
			out = consumers.getConsumer(consumer);
		} catch (MalformedURLException e) {
			throw new OAuthException(e);
		}
		String storePath = (String) out.getProperty("keystore.storePath");
		if (storePath != null) {
			Object key = out.getProperty(RSA_SHA1.PRIVATE_KEY);
			if (key == null) {
				// Load the key from the keystore
				InputStream stream = SimpleOAuth.class.getResourceAsStream(storePath.trim());
				if (stream == null) {
					throw new OAuthException("Keystore '" + storePath
							+ "' not found in the resource path");
				}
				KeyStore jks;
				try {
					jks = KeyStore.getInstance(KeyStore.getDefaultType());
				} catch (KeyStoreException e) {
					throw new OAuthException(e);
				}
				String keyAlias = (String) out.getProperty("keystore.keyAlias");
				String storePassword = (String) out.getProperty("keystore.storePassword");
				String keyPassword = (String) out.getProperty("keystore.keyPassword");
				if (keyAlias == null) {
					throw new OAuthException("Missing keyAlias for consumer '"
							+ consumer + "'");
				}
				if (storePassword == null) {
					throw new OAuthException(
							"Missing storePassword for consumer '" + consumer + "'");
				}
				if (keyPassword == null)
					keyPassword = storePassword;
				try {
					jks.load(stream, storePassword.toCharArray());
					stream.close();
					key = jks.getKey(keyAlias, keyPassword.toCharArray());
				} catch (NoSuchAlgorithmException e) {
					throw new OAuthException(e);
				} catch (CertificateException e) {
					throw new OAuthException(e);
				} catch (IOException e) {
					throw new OAuthException(e);
				} catch (KeyStoreException e) {
					throw new OAuthException(e);
				} catch (UnrecoverableKeyException e) {
					throw new OAuthException(e);
				}
				if (!(key instanceof PrivateKey)) {
					throw new OAuthException("Key '" + keyAlias
							+ "' for consumer '" + consumer
							+ "' is not a private key.");
				}
				out.setProperty(RSA_SHA1.PRIVATE_KEY, key);
			}
		}
		return out;
	}
	
	/**
	 * Get a new accessor for a consumer in the properties file.
	 */
	public OAuthAccessor newAccessor(String consumer) throws OAuthException {
		return new OAuthAccessor(getConsumer(consumer));
	}
	
	/**
	 * Get a new request token.
	 */
	public void setRequestToken(OAuthAccessor accessor)
	throws IOException, OAuthException {
		setRequestToken(accessor, OAuth.newList());
	}

	/**
	 * Get a new request token, with custom parameters in the request.
	 */
	public void setRequestToken(OAuthAccessor accessor, Collection<OAuth.Parameter> parameters)
	throws IOException, OAuthException {
		accessor.requestToken = null;
		accessor.accessToken = null;
		accessor.tokenSecret = null;
		{
			// This code supports the 'Variable Accessor Secret' extension
			// described in http://oauth.pbwiki.com/AccessorSecret
			Object accessorSecret = accessor
					.getProperty(OAuthConsumer.ACCESSOR_SECRET);
			if (accessorSecret != null) {
				parameters.add(new OAuth.Parameter("oauth_accessor_secret",
						accessorSecret.toString()));
			}
		}
		OAuthMessage response;
		try {
			response = invoke(accessor,
					accessor.consumer.serviceProvider.requestTokenURL,
					parameters, 10);
		} catch (URISyntaxException e) {
			throw new OAuthException(e);
		}
		response.requireParameters("oauth_token", "oauth_token_secret");
		accessor.requestToken = response.getParameter("oauth_token");
		accessor.tokenSecret = response.getParameter("oauth_token_secret");
	}
	
	/**
	 * Exchange a request token for an access token.
	 */
	public void setAccessToken(OAuthAccessor accessor)
	throws IOException, OAuthException {
		setAccessToken(accessor, OAuth.newList());
	}
	
	/**
	 * Exchange a request token for an access token, with custom parameters in the request.
	 */
	public void setAccessToken(OAuthAccessor accessor, Collection<OAuth.Parameter> parameters)
	throws IOException, OAuthException {
		OAuthMessage response;
		parameters.add(new OAuth.Parameter("oauth_token", accessor.requestToken));
		try {
			response = invoke(accessor,
					accessor.consumer.serviceProvider.accessTokenURL,
					parameters, 10);
		} catch (URISyntaxException e) {
			throw new OAuthException(e);
		}
		response.requireParameters("oauth_token", "oauth_token_secret");
		accessor.accessToken = response.getParameter("oauth_token");
		accessor.tokenSecret = response.getParameter("oauth_token_secret");
	}
	
	public URL getAuthorizationURL(OAuthAccessor accessor) throws IOException, OAuthException {
		return getAuthorizationURL(accessor, accessor.consumer.callbackURL);
	}
	
	public URL getAuthorizationURL(OAuthAccessor accessor, String callbackURL)
	throws IOException, OAuthException {
		String out = OAuth.addParameters(accessor.consumer.serviceProvider.userAuthorizationURL,
				"oauth_token", accessor.requestToken);
		if (callbackURL != null) {
			out = OAuth.addParameters(out,
				"oauth_callback", callbackURL);
		}
		return new URL(out);
	}
	
	public static void main(String[] args) throws IOException, OAuthException {
		// create a request token
		SimpleOAuth oauth;
		oauth = new SimpleOAuth("/oauth.properties");
		OAuthAccessor accessor = oauth.newAccessor("google");
		oauth.setRequestToken(accessor,
				OAuth.newList("scope", "http://www.google.com/calendar/feeds/"));
		// prompt the user for authorization
		BareBonesBrowserLaunch.openURL(
				 oauth.getAuthorizationURL(accessor).toExternalForm());
		 int ok = JOptionPane.showConfirmDialog(null,
				 "Did you authorize the token?");
		 if (ok != 0) System.exit(1);
		 // confirm authorization
		oauth.setAccessToken(accessor);
		System.out.println("'" + accessor.requestToken + "'");
		System.out.println("'" + accessor.tokenSecret + "'");
		System.out.println("'" + accessor.accessToken + "'");
	}
}
