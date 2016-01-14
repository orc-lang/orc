//
// SimpleOAuth.java -- Java class SimpleOAuth
// Project OrcSites
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.oauth;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
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
import net.oauth.client.OAuthClient;
import net.oauth.http.HttpResponseMessage;
import net.oauth.signature.RSA_SHA1;

/**
 * Wrapper around the OAuth libraries which should simplify using them. It's
 * simple because it gets all the information it needs upfront in a properties
 * file (or object). See the main method for an example.
 * <p>
 * All providers are configured with a properties file. A provider identified by
 * NAME must have the following properties defined:
 * <ul>
 * <li>NAME.serviceProvider.baseURL: prepended to all service URLs
 * <li>NAME.serviceProvider.requestTokenURL: URL to get a request token
 * <li>NAME.serviceProvider.userAuthorizationURL: redirect users to this URL to
 * authorize the request token
 * <li>NAME.serviceProvider.accessTokenURL: URL to exchange request for access
 * token
 * <li>NAME.consumerKey: oauth_consumer_key
 * </ul>
 * <p>
 * The following properties are optional:
 * <ul>
 * <li>NAME.consumerSecret: oauth_consumer_secret
 * <li>NAME.consumer.oauth_signature_method: oauth_signature_method
 * <li>NAME.consumer.httpMethod: POST or GET
 * </ul>
 * <p>
 * The following optional properties were added by this library and are not part
 * of the core OAuth library:
 * <ul>
 * <li>NAME.consumer.keystore.storePath: resource path to JKS file containing
 * signing key
 * <li>NAME.consumer.keystore.keyAlias: name of signing key
 * <li>NAME.consumer.keystore.storePassword: password for keystore
 * <li>NAME.consumer.keystore.keyPassword: password for key (if omitted, assumed
 * same as storePassword)
 * </ul>
 * <p>
 * See {@link ConsumerProperties} for more details on the properties file.
 *
 * @author quark
 */
public class SimpleOAuth {
    /**
     * HTTP client factory.
     */
    private final OAuthClient client = new OAuthClient(new net.oauth.client.httpclient3.HttpClient3());

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

    /**
     * Constructs an object of class SimpleOAuth, initializing with the given
     * properties.
     *
     * @param resource Resource name of file containing properties of this
     *            client/consumer
     * @throws IOException
     */
    public SimpleOAuth(final String resource) throws IOException {
        final URL resourceURL = SimpleOAuth.class.getResource(resource);
        if (resourceURL == null) {
            throw new IOException("Could not find resource '" + resource + "'");
        }
        consumers = new ConsumerProperties(ConsumerProperties.getProperties(resourceURL));
    }

    /**
     * Constructs an object of class SimpleOAuth, initializing with the given
     * properties.
     *
     * @param resource URL of file containing properties of this client/consumer
     * @throws IOException
     */
    public SimpleOAuth(final URL resource) throws IOException {
        this(ConsumerProperties.getProperties(resource));
    }

    /**
     * Constructs an object of class SimpleOAuth, initializing with the given
     * properties.
     *
     * @param properties Properties of this client/consumer
     */
    public SimpleOAuth(final Properties properties) {
        consumers = new ConsumerProperties(properties);
    }

    /**
     * Invoke an OAuth service with custom parameters while handling redirects
     * transparently.
     *
     * @param accessor
     * @param url
     * @param parameters
     * @param maxRedirects
     * @return OAuthMessage response
     * @throws IOException
     * @throws OAuthException
     * @throws URISyntaxException
     */
    private OAuthMessage invoke(final OAuthAccessor accessor, final String url, final Collection<? extends Map.Entry<?, ?>> parameters, final int maxRedirects) throws IOException, OAuthException, URISyntaxException {
        if (maxRedirects < 0) {
            throw new OAuthException("Maximum number of redirects reached");
        }
        try {
            return client.invoke(accessor, url, parameters);
        } catch (final OAuthProblemException e) {
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
            final String redirectUrl = (String) e.getParameters().get(HttpResponseMessage.LOCATION);
            // abort if redirect URL not found
            if (redirectUrl == null) {
                throw e;
            }
            return invoke(accessor, redirectUrl, parameters, maxRedirects - 1);
        }
    }

    /**
     * Get the consumer/client with the given name.
     *
     * @param consumer String name of consumer/client
     * @return OAuthConsumer, with private key property set from keystore
     * @throws OAuthException
     */
    public OAuthConsumer getConsumer(final String consumer) throws OAuthException {
        OAuthConsumer out;
        try {
            out = consumers.getConsumer(consumer);
        } catch (final MalformedURLException e) {
            throw new OAuthException(e);
        }
        final String storePath = (String) out.getProperty("keystore.storePath");
        if (storePath != null) {
            Object key = out.getProperty(RSA_SHA1.PRIVATE_KEY);
            if (key == null) {
                // Load the key from the keystore
                final InputStream stream = SimpleOAuth.class.getResourceAsStream(storePath.trim());
                if (stream == null) {
                    throw new OAuthException("Keystore '" + storePath + "' not found in the resource path");
                }
                KeyStore jks;
                try {
                    jks = KeyStore.getInstance(KeyStore.getDefaultType());
                } catch (final KeyStoreException e) {
                    throw new OAuthException(e);
                }
                final String keyAlias = (String) out.getProperty("keystore.keyAlias");
                final String storePassword = (String) out.getProperty("keystore.storePassword");
                String keyPassword = (String) out.getProperty("keystore.keyPassword");
                if (keyAlias == null) {
                    throw new OAuthException("Missing keyAlias for consumer '" + consumer + "'");
                }
                if (storePassword == null) {
                    throw new OAuthException("Missing storePassword for consumer '" + consumer + "'");
                }
                if (keyPassword == null) {
                    keyPassword = storePassword;
                }
                try {
                    jks.load(stream, storePassword.toCharArray());
                    stream.close();
                    key = jks.getKey(keyAlias, keyPassword.toCharArray());
                } catch (final NoSuchAlgorithmException e) {
                    throw new OAuthException(e);
                } catch (final CertificateException e) {
                    throw new OAuthException(e);
                } catch (final IOException e) {
                    throw new OAuthException(e);
                } catch (final KeyStoreException e) {
                    throw new OAuthException(e);
                } catch (final UnrecoverableKeyException e) {
                    throw new OAuthException(e);
                }
                if (!(key instanceof PrivateKey)) {
                    throw new OAuthException("Key '" + keyAlias + "' for consumer '" + consumer + "' is not a private key.");
                }
                out.setProperty(RSA_SHA1.PRIVATE_KEY, key);
            }
        }
        return out;
    }

    /**
     * Get a new accessor for a consumer/client in the properties file.
     *
     * @param consumer
     * @return OAuthAccessor for this consumer/client
     * @throws OAuthException
     */
    public OAuthAccessor newAccessor(final String consumer) throws OAuthException {
        return new OAuthAccessor(getConsumer(consumer));
    }

    /**
     * Get a new request token/temporary credentials. The consumer/client
     * obtains a set of temporary credentials from the server by making an
     * authenticated request to the Temporary Credential Request endpoint.
     *
     * @param accessor
     * @param callbackURL
     * @throws IOException
     * @throws OAuthException
     */
    public void obtainRequestToken(final OAuthAccessor accessor, final String callbackURL) throws IOException, OAuthException {
        obtainRequestToken(accessor, OAuth.newList(), callbackURL);
    }

    /**
     * Get a new request token/temporary credentials, with custom parameters in
     * the request. The consumer/client obtains a set of temporary credentials
     * from the server by making an authenticated request to the Temporary
     * Credential Request endpoint.
     *
     * @param accessor
     * @param parameters
     * @param callbackURL
     * @throws IOException
     * @throws OAuthException
     */
    public void obtainRequestToken(final OAuthAccessor accessor, final Collection<OAuth.Parameter> parameters, final String callbackURL) throws IOException, OAuthException {
        accessor.requestToken = null;
        accessor.accessToken = null;
        accessor.tokenSecret = null;
        if (callbackURL != null) {
            parameters.add(new OAuth.Parameter(OAuth.OAUTH_CALLBACK, callbackURL));
        }
        {
            // This code supports the 'Variable Accessor Secret' extension
            // described in http://oauth.pbwiki.com/AccessorSecret
            final Object accessorSecret = accessor.getProperty(OAuthConsumer.ACCESSOR_SECRET);
            if (accessorSecret != null) {
                parameters.add(new OAuth.Parameter(OAuthConsumer.ACCESSOR_SECRET, accessorSecret.toString()));
            }
        }
        OAuthMessage response;
        try {
            response = invoke(accessor, accessor.consumer.serviceProvider.requestTokenURL, parameters, 10);
        } catch (final URISyntaxException e) {
            throw new OAuthException(e);
        }
        response.requireParameters(OAuth.OAUTH_TOKEN, OAuth.OAUTH_TOKEN_SECRET);
        accessor.requestToken = response.getParameter(OAuth.OAUTH_TOKEN);
        accessor.tokenSecret = response.getParameter(OAuth.OAUTH_TOKEN_SECRET);
    }

    /**
     * Exchange a request token/temporary credentials for an access token/token
     * credentials. The consumer/client obtains a set of token credentials from
     * the server by making an authenticated request to the Token Credential
     * Request endpoint, using the temporary credentials.
     *
     * @param accessor
     * @throws IOException
     * @throws OAuthException
     */
    public void obtainAccessToken(final OAuthAccessor accessor) throws IOException, OAuthException {
        obtainAccessToken(accessor, OAuth.newList());
    }

    /**
     * Exchange a request token/temporary credentials for an access token/token
     * credentials, with custom parameters in the request. The consumer/client
     * obtains a set of token credentials from the server by making an
     * authenticated request to the Token Credential Request endpoint, using the
     * temporary credentials.
     *
     * @param accessor
     * @param parameters
     * @throws IOException
     * @throws OAuthException
     */
    public void obtainAccessToken(final OAuthAccessor accessor, final Collection<OAuth.Parameter> parameters) throws IOException, OAuthException {
        OAuthMessage response;
        parameters.add(new OAuth.Parameter(OAuth.OAUTH_TOKEN, accessor.requestToken));
        final String verifier = (String) accessor.getProperty("oauth_verifier");
        if (verifier != null) {
            parameters.add(new OAuth.Parameter("oauth_verifier", verifier));
        }
        try {
            response = invoke(accessor, accessor.consumer.serviceProvider.accessTokenURL, parameters, 10);
        } catch (final URISyntaxException e) {
            throw new OAuthException(e);
        }
        response.requireParameters(OAuth.OAUTH_TOKEN, OAuth.OAUTH_TOKEN_SECRET);
        accessor.accessToken = response.getParameter(OAuth.OAUTH_TOKEN);
        accessor.tokenSecret = response.getParameter(OAuth.OAUTH_TOKEN_SECRET);
    }

    /**
     * @param accessor
     * @return
     * @throws IOException
     * @throws OAuthException
     */
    public URL getAuthorizationURL(final OAuthAccessor accessor) throws IOException, OAuthException {
        return getAuthorizationURL(accessor, accessor.consumer.callbackURL);
    }

    /**
     * @param accessor
     * @param callbackURL
     * @return
     * @throws IOException
     * @throws OAuthException
     */
    public URL getAuthorizationURL(final OAuthAccessor accessor, final String callbackURL) throws IOException, OAuthException {
        String out = OAuth.addParameters(accessor.consumer.serviceProvider.userAuthorizationURL, OAuth.OAUTH_TOKEN, accessor.requestToken);
        if (callbackURL != null) {
            out = OAuth.addParameters(out, OAuth.OAUTH_CALLBACK, callbackURL);
        }
        return new URL(out);
    }

    /**
     * @param args
     * @throws IOException
     * @throws OAuthException
     * @throws URISyntaxException
     */
    public static void main(final String[] args) throws IOException, OAuthException, URISyntaxException {
        // create a request token
        SimpleOAuth oauth;
        oauth = new SimpleOAuth("/oauth.properties");
        final OAuthAccessor accessor = oauth.newAccessor("google");
        oauth.obtainRequestToken(accessor, OAuth.newList("scope", "http://www.google.com/calendar/feeds/"), null);
        // prompt the user for authorization
        Desktop.getDesktop().browse(new URI(oauth.getAuthorizationURL(accessor).toExternalForm()));
        final int ok = JOptionPane.showConfirmDialog(null, "Did you authorize the token?");
        if (ok != 0) {
            System.exit(1);
        }
        // confirm authorization
        oauth.obtainAccessToken(accessor);
        System.out.println("'" + accessor.requestToken + "'");
        System.out.println("'" + accessor.tokenSecret + "'");
        System.out.println("'" + accessor.accessToken + "'");
    }
}
