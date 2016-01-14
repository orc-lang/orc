//
// GoogleSearchFactory.java -- Java class GoogleSearchFactory
// Project OrcSites
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
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Properties;

import scala.collection.immutable.List;

import orc.error.runtime.JavaException;
import orc.error.runtime.TokenException;
import orc.values.sites.Site;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.EvalSite;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * Wrapper for the Google Search AJAX API described at
 * http://code.google.com/apis/ajaxsearch/documentation/#fonje Returns a list of
 * pages, where each page is a site which returns a list of result GSearch
 * objects. See the Google Search AJAX API documentation for details. Properties
 * include:
 * <ul>
 * <li>unescapedUrl
 * <li>url
 * <li>visibleUrl
 * <li>cacheUrl
 * <li>title
 * <li>titleNoFormatting
 * <li>content
 * </ul>
 *
 * @author quark
 */
public class GoogleSearchFactory extends EvalSite {
    private static class GoogleSearch extends EvalSite {
        private final static String apiURL = "http://ajax.googleapis.com/ajax/services/search/web";
        private final String apiKey;
        private final String httpReferer;

        public GoogleSearch(final String file) throws IOException {
            final Properties p = new Properties();
            final InputStream stream = GoogleSearch.class.getResourceAsStream(file);
            if (stream == null) {
                throw new FileNotFoundException(file);
            }
            p.load(stream);
            apiKey = p.getProperty("orc.lib.net.google.key");
            httpReferer = p.getProperty("orc.lib.net.google.referer");
        }

        @Override
        public Object evaluate(final Args args) throws TokenException {
            final String url;
            final JSONArray results;
            final JSONArray pages;
            // get the first page of results and the cursor
            try {
                final String search = args.stringArg(0);
                url = apiURL + "?v=1.0" + "&q=" + URLEncoder.encode(search, "UTF-8") + "&key=" + apiKey;
                final JSONObject root = requestJSON(new URL(url));
                final JSONObject response = root.getJSONObject("responseData");
                results = response.getJSONArray("results");
                pages = response.getJSONObject("cursor").getJSONArray("pages");
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

            // build a list of pages
            List<Site> out = nilList();
            for (int i = pages.length() - 1; i > 1; --i) {
                final String page;
                try {
                    page = pages.getJSONObject(i).getString("start");
                    out = makeCons(new EvalSite() {
                        @Override
                        public Object evaluate(final Args args) throws TokenException {
                            JSONObject root;
                            try {
                                root = requestJSON(new URL(url + "&start=" + page));
                                final JSONObject response = root.getJSONObject("responseData");
                                final JSONArray results = response.getJSONArray("results");
                                return JSONSite.wrapJSON(results);
                            } catch (final MalformedURLException e) {
                                // should be impossible
                                throw new AssertionError(e);
                            } catch (final IOException e) {
                                throw new JavaException(e);
                            } catch (final JSONException e) {
                                throw new JavaException(e);
                            }
                        }
                    }, out);
                } catch (final JSONException e) {
                    // Skip bad or missing cursors
                }
            }
            // the first page, we already got the results, so it is much simpler
            out = makeCons(new EvalSite() {
                @Override
                public Object evaluate(final Args args) throws TokenException {
                    return JSONSite.wrapJSON(results);
                }
            }, out);
            return out;
        }

        /** Utility method to make a JSON request. */
        private JSONObject requestJSON(final URL url) throws IOException, JSONException {
            final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Referer", httpReferer);
            conn.setConnectTimeout(10000); // 10 seconds is reasonable
            conn.setReadTimeout(5000); // 5 seconds is reasonable
            conn.connect();
            final StringBuilder content = new StringBuilder();
            final InputStreamReader in = new InputStreamReader(conn.getInputStream(), "UTF-8");
            final char[] buff = new char[1024];
            while (true) {
                final int blen = in.read(buff);
                if (blen < 0) {
                    break;
                }
                content.append(buff, 0, blen);
            }
            in.close();
            return new JSONObject(content.toString());
        }
    }

    @Override
    public Object evaluate(final Args args) throws TokenException {
        try {
            return new GoogleSearch("/" + args.stringArg(0));
        } catch (final IOException e) {
            throw new JavaException(e);
        }
    }
}
