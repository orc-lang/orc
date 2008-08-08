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
import orc.runtime.sites.ThreadedSite;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class YahooSearchFactory extends EvalSite {
	private static class YahooSearch extends ThreadedSite {
		private final static String apiURL = "http://search.yahooapis.com/WebSearchService/V1/webSearch";
		private final String appid;
		public YahooSearch(String file) throws IOException {
			Properties p = new Properties();
			InputStream stream = YahooSearch.class.getResourceAsStream(file);
			if (stream == null) throw new FileNotFoundException(file);
			p.load(stream);
			appid = p.getProperty("orc.lib.net.yahoo.appid");
		}
		@Override
		public Object evaluate(Args args) throws TokenException {
			// get the first page of results and the cursor
			try {
				String search = args.stringArg(0);
				int numResults = 10;
				if (args.size() > 1) numResults = args.intArg(1);
				String url = apiURL +
						"?query=" + URLEncoder.encode(search, "UTF-8") +
						"&results=" + numResults +
						"&appid=" + appid +
						"&output=json";
				JSONObject root = requestJSON(new URL(url));
				JSONObject response = root.getJSONObject("ResultSet");
				return JSONSite.wrapJSON(response.getJSONArray("Result"));
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
		}
		/** Utility method to make a JSON request. */
		private JSONObject requestJSON(URL url) throws IOException, JSONException {
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
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
			return new YahooSearch("/" + args.stringArg(0));
		} catch (IOException e) {
			throw new JavaException(e);
		}
	}	
}
