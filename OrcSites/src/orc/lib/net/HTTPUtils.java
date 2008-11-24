package orc.lib.net;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class HTTPUtils {
	public static String getURL(URL url) throws IOException {
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
		return content.toString();
	}
	public static String postURL(URL url, String request) throws IOException {
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		conn.setConnectTimeout(10000); // 10 seconds is reasonable
		conn.setReadTimeout(5000); // 5 seconds is reasonable
		conn.connect();
		OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
		out.write(request);
		out.close();
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
		return content.toString();
	}
}
