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

import orc.error.OrcError;
import orc.error.runtime.JavaException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.Site;
import orc.runtime.sites.ThreadedSite;
import orc.runtime.values.ConsValue;
import orc.runtime.values.ListValue;
import orc.runtime.values.NilValue;
import orc.runtime.values.Value;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * Wrapper for the Google Search AJAX API described at
 * http://code.google.com/apis/ajaxsearch/documentation/#fonje
 * 
 * Returns a list of pages, where each page is a site which
 * returns a list of result GSearch objects. See the Google Search
 * AJAX API documentation for details. Properties include:
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
	private static class GoogleSearch extends ThreadedSite {
		private final static String apiURL = "http://ajax.googleapis.com/ajax/services/search/web";
		private final String apiKey;
		private final String httpReferer;
		
		public GoogleSearch(String file) throws IOException {
			Properties p = new Properties();
			InputStream stream = GoogleSearch.class.getResourceAsStream(file);
			if (stream == null) throw new FileNotFoundException(file);
			p.load(stream);
			apiKey = p.getProperty("orc.lib.net.google.key");
			httpReferer = p.getProperty("orc.lib.net.google.referer");
		}
		
		@Override
		public Value evaluate(Args args) throws TokenException {
			final String url;
			final JSONArray results;
			final JSONArray pages;
			// get the first page of results and the cursor
			try {
				String search = args.stringArg(0);
				url = apiURL + "?v=1.0" +
						"&q=" + URLEncoder.encode(search, "UTF-8") +
						"&key=" + apiKey;
				JSONObject root = requestJSON(new URL(url));
				JSONObject response = root.getJSONObject("responseData");
				results = response.getJSONArray("results");
				pages = response.getJSONObject("cursor").getJSONArray("pages");
			} catch (UnsupportedEncodingException e) {
				// should be impossible
				throw new OrcError(e);
			} catch (MalformedURLException e) {
				// should be impossible
				throw new OrcError(e);
			} catch (IOException e) {
				throw new JavaException(e);
			} catch (JSONException e) {
				throw new JavaException(e);
			}
			
			// build a list of pages
			ListValue out = NilValue.singleton;
			for (int i = pages.length()-1; i > 1; --i) {
				final String page;
				try {
					page = pages.getJSONObject(i).getString("start");
					out = new ConsValue<Site>(new ThreadedSite() {
						@Override
						public Object evaluate(Args args) throws TokenException {
							JSONObject root;
							try {
								root = requestJSON(new URL(url + "&start=" + page));
								JSONObject response = root.getJSONObject("responseData");
								JSONArray results = response.getJSONArray("results");
								return JSONSite.wrapJSON(results);
							} catch (MalformedURLException e) {
								// should be impossible
								throw new OrcError(e);
							} catch (IOException e) {
								throw new JavaException(e);
							} catch (JSONException e) {
								throw new JavaException(e);
							}
						}
					}, out);
				} catch (JSONException e) {
					// Skip bad or missing cursors
				}
			}
			// the first page, we already got the results, so it is much simpler
			out = new ConsValue<Site>(new EvalSite() {
				@Override
				public Object evaluate(Args args) throws TokenException {
					return JSONSite.wrapJSON(results);
				}
			}, out);
			return out;
		}
		
		/** Utility method to make a JSON request. */
		private JSONObject requestJSON(URL url) throws IOException, JSONException {
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			conn.setRequestProperty("Referer", httpReferer);
			conn.setConnectTimeout(10000); // 10 seconds is reasonable
			conn.setReadTimeout(5000); // 5 seconds is reasonable
			conn.connect();
			StringBuilder content = new StringBuilder();
			InputStreamReader in = new InputStreamReader(conn.getInputStream(), "UTF-8");
			char[] buff = new char[1024];
			while (true) {
				int blen = in.read(buff);
				if (blen < 0) break;
				content.append(buff, 0, blen);
			}
			in.close();
			return new JSONObject(content.toString());
		}
	}

	@Override
	public Object evaluate(Args args) throws TokenException {
		try {
			return new GoogleSearch("/" + args.stringArg(0));
		} catch (IOException e) {
			throw new JavaException(e);
		}
	}
}
