package orc.lib.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Callable;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import kilim.Pausable;

import orc.runtime.Kilim;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class XMLUtils {
	public static DocumentBuilderFactory builderFactory;
	static {
		builderFactory = DocumentBuilderFactory.newInstance();
		builderFactory.setCoalescing(true);
	}
	
	/**
	 * Utility method to run a blocking operation.
	 * FIXME: duplicates code from HTTPUtils
	 */
	private static <E> E runThreaded(Callable<E> thunk) throws IOException, SAXException, Pausable {
		try {
			return Kilim.runThreaded(thunk);
		} catch (Exception e) {
			// HACK: for some reason when I put these
			// as separate catch clauses it doesn't work like I expect
			if (e instanceof IOException) {
				throw (IOException)e;
			} else if (e instanceof SAXException) {
				throw (IOException)e;
			} else if (e instanceof RuntimeException) {
				throw (RuntimeException)e;
			} else {
				throw new AssertionError(e);
			}
		}
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
	
	public static Document getURL(final URL url) throws IOException, SAXException, Pausable {
		final DocumentBuilder builder;
		try {
			builder = builderFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			// should never happen
			throw new AssertionError(e);
		}
		return runThreaded(new Callable<Document>() {
			public Document call() throws IOException, SAXException {
				HttpURLConnection conn = HTTPUtils.connect(url, false);
				InputStream in = conn.getInputStream();
				Document doc = builder.parse(conn.getInputStream());
				in.close();
				conn.disconnect();
				return doc;
			}
		});
	}

	public static Document postURL(final URL url, final String request) throws IOException, SAXException, Pausable {
		final DocumentBuilder builder;
		try {
			builder = builderFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			// should never happen
			throw new AssertionError(e);
		}
		return runThreaded(new Callable<Document>() {
			public Document call() throws IOException, SAXException {
				HttpURLConnection conn = HTTPUtils.connect(url, true);
				OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
				out.write(request);
				out.close();
				InputStream in = conn.getInputStream();
				Document doc = builder.parse(conn.getInputStream());
				in.close();
				conn.disconnect();
				return doc;
			}
		});
	}
}
