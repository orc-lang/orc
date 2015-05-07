package orc.lib.net;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.codehaus.jettison.json.JSONException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class XMLUtils {
	public static DocumentBuilderFactory builderFactory;
	static {
		builderFactory = DocumentBuilderFactory.newInstance();
		builderFactory.setCoalescing(true);
	}
	public static String escapeXML(String text) {
		StringBuilder sb = new StringBuilder();
		int len = text.length();
		for (int i = 0; i < len; i++) {
			char c = text.charAt(i);
			switch (c) {
			case 34: sb.append("&quot;"); break;
			case 38: sb.append("&amp;"); break;
			case 39: sb.append("&apos;"); break;
			case 60: sb.append("&lt;"); break;
			case 62: sb.append("&gt;"); break;
			default:
				if (c > 0x7F) {
					sb.append("&#");
					sb.append(Integer.toString(c, 10));
					sb.append(';');
				} else {
					sb.append(c);
				}
			}
		}
		return sb.toString();
	}

	public static Document postURL(URL url, String request) throws IOException, JSONException, ParserConfigurationException, SAXException {
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		conn.setConnectTimeout(10000); // 10 seconds is reasonable
		conn.setReadTimeout(5000); // 5 seconds is reasonable
		conn.setDoOutput(true);
		conn.connect();
		OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
		out.write(request);
		out.close();
		DocumentBuilder builder = builderFactory.newDocumentBuilder();
		Document doc = builder.parse(conn.getInputStream());
		conn.disconnect();
		return doc;
	}
}
