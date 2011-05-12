//
// YahooSearchFactory.java -- Java class YahooSearchFactory
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

import orc.error.runtime.JavaException;
import orc.error.runtime.TokenException;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.EvalSite;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * See examples/yahoo.orc
 * <p>
 * http://developer.yahoo.com/search/web/V1/webSearch.html
 * @author quark
 */
public class YahooSearchFactory extends EvalSite {
	private static class YahooSearch extends EvalSite {
		private final static String apiURL = "http://search.yahooapis.com/WebSearchService/V1/webSearch";
		private final String appid;

		public YahooSearch(final String file) throws IOException {
			final Properties p = new Properties();
			final InputStream stream = YahooSearch.class.getResourceAsStream(file);
			if (stream == null) {
				throw new FileNotFoundException(file);
			}
			p.load(stream);
			appid = p.getProperty("orc.lib.net.yahoo.appid");
		}

		@Override
		public Object evaluate(final Args args) throws TokenException {
			// get the first page of results and the cursor
			try {
				final String search = args.stringArg(0);
				int numResults = 10;
				if (args.size() > 1) {
					numResults = args.intArg(1);
				}
				final String url = apiURL + "?query=" + URLEncoder.encode(search, "UTF-8") + "&results=" + numResults + "&appid=" + appid + "&output=json";
				final String data = HTTPUtils.getURL(new URL(url));
				final JSONObject root = new JSONObject(data);
				final JSONObject response = root.getJSONObject("ResultSet");
				return JSONSite.wrapJSON(response.getJSONArray("Result"));
			} catch (final UnsupportedEncodingException e) {
				// should be impossible
				throw new AssertionError(e);
			} catch (final MalformedURLException e) {
				// should be impossible
				throw new AssertionError(e);
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
			return new YahooSearch("/" + args.stringArg(0));
		} catch (final IOException e) {
			throw new JavaException(e);
		}
	}
}
