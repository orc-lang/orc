package orc.lib.net;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Callable;

import orc.runtime.Kilim;

import kilim.Pausable;

public class HTTPUtils {
	private HTTPUtils() {}
	
	public static HttpURLConnection connect(URL url, boolean output) throws IOException {
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			conn.setRequestProperty("User-Agent", FF_UA);
			conn.setConnectTimeout(10000); // 10 seconds is reasonable
			conn.setReadTimeout(5000); // 5 seconds is reasonable
			conn.setDoOutput(output);
			conn.connect();
			return conn;
	}
	
	/**
	 * Utility method to run a blocking operation.
	 */
	private static <E> E runThreaded(Callable<E> thunk) throws IOException, Pausable {
		try {
			return Kilim.runThreaded(thunk);
		} catch (Exception e) {
			// HACK: for some reason when I put these
			// as separate catch clauses it doesn't work like I expect
			if (e instanceof IOException) {
				throw (IOException)e;
			} else if (e instanceof RuntimeException) {
				throw (RuntimeException)e;
			} else {
				throw new AssertionError(e);
			}
		}
	}
	
	private static String FF_UA = "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.4) Gecko/2008111317 Ubuntu/8.04 (hardy) Firefox/3.0.4";
	
	public static String getURL(final URL url) throws IOException, Pausable {
		return runThreaded(new Callable<String>() {
			public String call() throws IOException {
				HttpURLConnection conn = connect(url, false);
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
		});
	}
	
	public static String postURL(final URL url, final String request) throws IOException, Pausable {
		return runThreaded(new Callable<String>() {
			public String call() throws IOException {
				HttpURLConnection conn = connect(url, true);
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
		});
	}
}
