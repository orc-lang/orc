package orc.orcx;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 
 * Centralized class for handling various aspects of the http protocol between Orc and Galax
 * and between Orc servers.
 * 
 * @author kmorton
 * */

public class HTTPUtils {
	// The following definitions are for sending data via HTTP (to galax, other Orc servers, etc.)
	public enum GalaxCommand { compile_query, execute_query, xml_to_string, string_to_xml };
	
	public static String send_galax(String url_base, GalaxCommand command, String data) {
		return sendData(url_base, "orcx-" + command, data);
	}
	
	public static String sendData(String url_base, String target, String data) {
		String results = "";
        String newline = null;  // Find the newline character(s) on the current system
        try {
            newline = System.getProperty("line.separator");
        } catch (Exception e) {
            newline = "\n";
        }

		try {
			URL url = new URL(url_base + "/" + target);  //("http://localhost:3001/orcx-compile_query");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setAllowUserInteraction(false);
			conn.setDoOutput(true);
			conn.setRequestProperty( "Content-Type", "text/plain" );
			conn.setRequestProperty( "Content-Length", Integer.toString(data.length()));

			// get the output stream to POST our form data
			OutputStream rawOutStream = conn.getOutputStream();
			PrintWriter pw = new PrintWriter(rawOutStream);

			pw.print(data);
			pw.flush();
			pw.close();

			InputStream rawInStream = conn.getInputStream();

			// Concatenate response lines and store in 'results'
			BufferedReader rdr = new BufferedReader(new InputStreamReader(rawInStream));
			String line;
			while ((line = rdr.readLine()) != null) {
				results += line + newline;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return results;
	}
}
