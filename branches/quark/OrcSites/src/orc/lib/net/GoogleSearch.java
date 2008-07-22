package orc.lib.net;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import orc.error.OrcError;
import orc.error.TokenException;
import orc.runtime.Args;
import orc.runtime.Continuation;
import orc.runtime.Thunk;
import orc.runtime.sites.ThreadedSite;
import orc.runtime.values.LazyListValue;
import orc.runtime.values.NoneValue;
import orc.runtime.values.OptionValue;
import orc.runtime.values.SomeValue;
import orc.runtime.values.TupleValue;
import orc.runtime.values.Value;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * Wrapper for the Google Search AJAX API described at
 * http://code.google.com/apis/ajaxsearch/documentation/#fonje
 * 
 * Returns a list of GSearch result objects. See the Google Search
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
public class GoogleSearch extends ThreadedSite {
	private final static String apiURL = "http://ajax.googleapis.com/ajax/services/search/web";
	/** FIXME: this should be configurable, i.e. via properties file */
	private final static String apiKey = "ABQIAAAAbOrQTsQ17iqKhS7T9kLkoxRsm5oUtab7vAoqeCz6MopXJb74FBR3r5d5u7lNXF4us6VBGvnDEurlmw";
	/** FIXME: this should be configurable, i.e. via properties file */
	private final static String httpReferer = "http://orc.csres.utexas.edu/orchard";
	
	public static class ResultsThunk implements Thunk<OptionValue<TupleValue>> {
		private String url;
		private int page;
		private JSONArray results;
		private int maxPages;
		private int index;
		public ResultsThunk(String url) {
			this(url, 0);
		}
		public ResultsThunk(String url, int page) {
			this(url, page, null, 0, 0);
		}
		private ResultsThunk(String url, int page, JSONArray results, int index, int maxPages) {
			this.url = url;
			this.page = page;
			this.results = results;
			this.index = index;
			this.maxPages = maxPages;
		}
		public OptionValue<TupleValue> apply() {
			if (results != null) {
				// We have results already;
				// skip to the next available one
    			while (index < results.length()) {
    				try {
    					Object result = results.get(index);
    					ResultsThunk next = new ResultsThunk(
    							url, page, results, index+1, maxPages);
    					// return the current result
        				return new SomeValue<TupleValue>(new TupleValue(
        						JSONSite.wrapJSON(result), new LazyListValue(next)));
    				} catch (JSONException e) {
        				index++;
    				}
    			}
    			// If no more are available, go to the next page
    			if (page+1 >= maxPages) {
    				return new NoneValue<TupleValue>();
    			} else {
        			return new ResultsThunk(url, page+1).apply();
    			}
			} else {
				// We don't have any results yet
    			final Continuation c = Continuation.suspend();
    			new Thread() {
    				public void run() {
            			try {
            				JSONObject root = requestJSON(new URL(url + "&start=" + page));
                			JSONObject response = root.getJSONObject("responseData");
                			results = response.getJSONArray("results");
                			maxPages = response.getJSONObject("cursor").getJSONArray("pages").length();
                			c.resume(apply());
            			} catch (MalformedURLException e) {
            				c.error(e);
            			} catch (IOException e) {
            				c.error(e);
            			} catch (JSONException e) {
            				c.error(e);
            			}
    				}
    			}.start();
    			return null;
			}
		}
	}
	
	@Override
	public Value evaluate(Args args) throws TokenException {
		try {
			String search = args.stringArg(0);
			String url = apiURL + "?v=1.0" +
					"&q=" + URLEncoder.encode(search, "UTF-8") +
					"&key=" + apiKey;
			return new LazyListValue(new ResultsThunk(url, 0));
		} catch (UnsupportedEncodingException e) {
			// should be impossible
			throw new OrcError(e);
		}
	}
	
	/** Utility method to make a JSON request. */
	private static JSONObject requestJSON(URL url) throws IOException, JSONException {
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
