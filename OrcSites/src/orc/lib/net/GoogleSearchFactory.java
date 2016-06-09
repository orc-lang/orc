//
// GoogleSearchFactory.java -- Java class GoogleSearchFactory
// Project OrcSites
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
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

import orc.error.runtime.JavaException;
import orc.error.runtime.TokenException;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.EvalSite;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * Wrapper for the Google Search AJAX API described at
 * https://developers.google.com/custom-search/json-api/v1/reference/cse
 * <p>
 * Returns a list of result item objects. See the Google Search JSON API
 * documentation for details. Properties include:
 * <ul>
 * <li>title
 * <li>htmlTitle
 * <li>link
 * <li>displayLink
 * <li>snippet
 * <li>htmlSnippet
 * <li>mime
 * <li>fileFormat
 * <li>formattedUrl
 * <li>htmlFormattedUrl
 * </ul>
 *
 * @author quark
 */
public class GoogleSearchFactory extends EvalSite {
    private static class GoogleSearch extends EvalSite {
        private final static String apiURL = "https://www.googleapis.com/customsearch/v1";
        private final String apiKeyName;
        private final String apiKeyValue;
        private final String googleCustomSearchEngineID;

        public GoogleSearch(final String file) throws IOException {
            final Properties p = new Properties();
            final InputStream stream = GoogleSearch.class.getResourceAsStream(file);
            if (stream == null) {
                throw new FileNotFoundException(file);
            }
            p.load(stream);
            apiKeyName = p.getProperty("webservice.google.parameter.name");
            apiKeyValue = p.getProperty("webservice.google.parameter.value");
            googleCustomSearchEngineID = p.getProperty("orc.lib.net.google.searchEngineID");
        }

        @Override
        public Object evaluate(final Args args) throws TokenException {
            final JSONArray results;
            // get the first page of results and the cursor
            try {
                final String search = args.stringArg(0);
                final Number startIndex = args.numberArg(1);
                final String url = apiURL + "?" + apiKeyName + "=" + apiKeyValue + "&cx=" + googleCustomSearchEngineID + "&q=" + URLEncoder.encode(search, "UTF-8") + "&start=" + startIndex.toString();
                final JSONObject response = requestJSON(new URL(url));
                System.err.println(response.toString());
                results = response.getJSONArray("items");
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
            return results;
        }

        /** Utility method to make a JSON request. */
        private JSONObject requestJSON(final URL url) throws IOException, JSONException {
            final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
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
