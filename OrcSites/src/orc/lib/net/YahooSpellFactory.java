//
// YahooSpellFactory.java -- Java class YahooSpellFactory
// Project OrcSites
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.net;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Properties;

import kilim.Pausable;
import orc.error.OrcError;
import orc.error.runtime.JavaException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.KilimSite;
import orc.runtime.values.ConsValue;
import orc.runtime.values.NilValue;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * http://developer.yahoo.com/search/web/V1/spellingSuggestion.html
 * @author quark
 */
public class YahooSpellFactory extends EvalSite {
	private static class YahooSpell extends KilimSite {
		private final static String apiURL = "http://search.yahooapis.com/WebSearchService/V1/spellingSuggestion";
		private final String appid;

		public YahooSpell(final String file) throws IOException {
			final Properties p = new Properties();
			final InputStream stream = YahooSpell.class.getResourceAsStream(file);
			if (stream == null) {
				throw new FileNotFoundException(file);
			}
			p.load(stream);
			appid = p.getProperty("orc.lib.net.yahoo.appid");
		}

		@Override
		public Object evaluate(final Args args) throws TokenException, Pausable {
			// get the first page of results and the cursor
			try {
				final String search = args.stringArg(0);
				final String url = apiURL + "?query=" + URLEncoder.encode(search, "UTF-8") + "&appid=" + appid + "&output=json";
				final String data = HTTPUtils.getURL(new URL(url));
				final JSONObject root = new JSONObject(data);
				final Object response = root.get("ResultSet");
				if (response instanceof String) {
					// indicates no result was returned
					return NilValue.singleton;
				} else {
					return new ConsValue<String>(((JSONObject) response).getString("Result"), NilValue.singleton);
				}
			} catch (final ClassCastException e) {
				// should be impossible
				throw new OrcError(e);
			} catch (final UnsupportedEncodingException e) {
				// should be impossible
				throw new OrcError(e);
			} catch (final MalformedURLException e) {
				// should be impossible
				throw new OrcError(e);
			} catch (final IOException e) {
				throw new JavaException(e);
			} catch (final JSONException e) {
				throw new JavaException(e);
			}
		}
	}

	@Override
	public Object evaluate(final Args args) throws TokenException {
		try {
			return new YahooSpell("/" + args.stringArg(0));
		} catch (final IOException e) {
			throw new JavaException(e);
		}
	}
}
