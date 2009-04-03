package orc.lib.orchard;

import java.net.MalformedURLException;
import java.net.URL;

import orc.error.runtime.JavaException;
import orc.error.runtime.SiteException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.OrcEngine;
import orc.runtime.Token;
import orc.runtime.sites.Site;

/**
 * Redirect the user to a URL.
 * @author quark
 */
public class Redirect extends Site {
	/**
	 * Interface implemented by an engine which can handle
	 * this site.
	 * @author quark
	 */
	public interface Redirectable {
		public void redirect(URL url);
	}
	@Override
	public void callSite(Args args, final Token caller) throws TokenException {
		OrcEngine engine = caller.getEngine();
		final String url = args.stringArg(0);
		if (!(engine instanceof Redirectable)) {
			caller.error(new SiteException(
					"This Orc engine does not support the Redirect site."));
		}
		try {
			((Redirectable)engine).redirect(new URL(url));
			caller.resume(signal());
		} catch (MalformedURLException e) {
			caller.error(new JavaException(e));
		}
	}
}
