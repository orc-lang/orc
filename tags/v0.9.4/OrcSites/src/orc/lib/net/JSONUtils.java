package orc.lib.net;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class JSONUtils {
	/** Utility method to make a JSON request. */
	public static JSONObject getURL(URL url) throws IOException, JSONException {
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
		conn.disconnect();
		return new JSONObject(content.toString());
	}
}
