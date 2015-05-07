package orc.lib.fun;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import orc.error.runtime.SiteException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.PartialSite;

public class Eliza extends EvalSite {
	@Override
	public Object evaluate(Args args) throws TokenException {
		final net.chayden.eliza.Eliza eliza = new net.chayden.eliza.Eliza();
		String script;
		if (args.size() > 0) {
			script = "/" + args.stringArg(0);
		} else {
			script = "/net/chayden/eliza/eliza.script";
		}
		InputStream stream = Eliza.class.getResourceAsStream(script);
		if (stream == null) {
			throw new SiteException("Eliza script '" + script + "' not found.");
		}
		try {
    		eliza.readScript(new InputStreamReader(stream));
		} catch (IOException e) {
			throw new SiteException("Error reading script '"+script+"'", e);
		}
		return new PartialSite() {
			@Override
			public Object evaluate(Args args) throws TokenException {
				synchronized (eliza) {
    				if (eliza.finished()) return null;
    				try {
						return eliza.processInput(args.stringArg(0));
					} catch (IOException e) {
						throw new SiteException("Error processing script", e);
					}
				}
			}
		};
	}
}
