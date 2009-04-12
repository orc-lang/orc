package orc.lib.ui;

import java.io.IOException;


import orc.error.runtime.JavaException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;


public class OAuthProviderSite extends EvalSite {
	@Override
	public Object evaluate(Args args) throws TokenException {
		try {
			/**
			 * This implementation of OAuthProvider 
			 */
			return new GuiOAuthProvider(
					// force root-relative resource path
					"/" + args.stringArg(0));
		} catch (IOException e) {
			throw new JavaException(e);
		}
	}
}
