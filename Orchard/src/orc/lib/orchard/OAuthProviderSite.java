package orc.lib.orchard;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.Callable;

import kilim.Mailbox;
import kilim.pausable;
import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import orc.error.JavaException;
import orc.error.SiteException;
import orc.error.TokenException;
import orc.lib.orchard.Redirect.Redirectable;
import orc.oauth.OAuthProvider;
import orc.orchard.OrchardOAuthServlet;
import orc.orchard.Job.ProvidesGlobals;
import orc.runtime.Args;
import orc.runtime.Kilim;
import orc.runtime.OrcEngine;
import orc.runtime.Token;
import orc.runtime.sites.Site;
import orc.runtime.values.Constant;

public class OAuthProviderSite extends Site {
	public static class PendingOAuthAccessor {
		public OAuthAccessor accessor;
		public Mailbox<OAuthAccessor> ready;
	}
	/**
	 * This provider uses Java UI stuff to launch a browser
	 * and prompt the user to confirm authorization.
	 */
	private static class WebOAuthProvider extends OAuthProvider {
		private ProvidesGlobals globals;
		private Redirectable redirectable;
		public WebOAuthProvider(ProvidesGlobals globals,
				Redirectable redirectable, String properties)
		throws IOException {
			super(properties);
			this.globals = globals;
			this.redirectable = redirectable;
		}

		@Override
		public @pausable OAuthAccessor authenticate(final String consumer,
				final List<OAuth.Parameter> request)
		throws Exception {
			final OAuthAccessor accessor = oauth.newAccessor(consumer);
			// get a request token
			Kilim.runThreaded(new Callable() {
				public Object call() throws Exception {
					oauth.setRequestToken(accessor, request);
					return Kilim.signal;
				}
			});
			// request authorization and wait for response
			Mailbox ready = new Mailbox();
			redirectable.redirect(oauth.getAuthorizationURL(accessor,
					OrchardOAuthServlet.getCallbackURL(accessor, ready, globals)));
			ready.get();
			// get the access token
			Kilim.runThreaded(new Callable() {
				public Object call() throws Exception {
					oauth.setAccessToken(accessor);
					return Kilim.signal;
				}
			});
			return accessor;
		}
	}
	
	@Override
	public void callSite(Args args, Token caller) throws TokenException {
		OrcEngine engine = caller.getEngine();
		if (!(engine instanceof ProvidesGlobals)) {
			throw new SiteException(
					"This site is not supported on the engine " +
					engine.getClass().toString());
		}
		if (!(engine instanceof Redirectable)) {
			throw new SiteException(
					"This site is not supported on the engine " +
					engine.getClass().toString());
		}
		try {
			/**
			 * This implementation of OAuthProvider 
			 */
			caller.resume(new Constant(new WebOAuthProvider(
					(ProvidesGlobals)engine,
					(Redirectable)engine,
					// force root-relative resource path
					"/" + args.stringArg(0))));
		} catch (IOException e) {
			throw new JavaException(e);
		}
	}
	
	@SuppressWarnings("unused")
	private static class MockGlobals implements ProvidesGlobals {
		public String addGlobal(Object object) {
			System.out.println("Adding " + object.toString());
			return "KEY";
		}
	}
	
	@SuppressWarnings("unused")
	private static class MockRedirectable implements Redirectable {
		public void redirect(URL url) {
			System.out.println(url.toExternalForm());
		}
	}
}
