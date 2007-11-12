package orc.orcx;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 
 * Centralized class for handling interaction between Orc servers via HTTP
 * Implements the send/receive mechanism between Orc servers.
 * 
 * @author kmorton
 */

public class OrcHTTPServer implements Runnable {
	static private OrcHTTPServer singleton = null;
	static private int listenPort = 3100;  // default port is 4000 for testing 
	static private ServerSocket listener = null;
	static public LinkedBlockingQueue<String> queue = null;  // Support other datatypes than String?
	
	// Private constructor for singleton pattern
	private OrcHTTPServer() {
		queue = new LinkedBlockingQueue<String>();
	}
	
	public static OrcHTTPServer getServer() {
		if (singleton == null) {
			singleton = new OrcHTTPServer();
		}
		return singleton;
	}
	
	// Set the port if the server hasn't yet started
	public static void setPort(int port) throws Exception {
		if (singleton == null) {
			listenPort = port;
		}
		else {
			throw new Exception("Error: Attempted to change the server port after the server has started.");
		}
	}
	
	public static void terminate() {
		try {
			System.err.println("Trying to close listener");
			listener.close();
			System.err.println("Done closing listener");
		} catch (IOException e) {
			System.err.println("Warning: Unable to cleanly terminate the open socket, caught exception:");
			System.err.println(e);
		}
	}

	// Start the HTTP Server in a polling loop, writing data to a LinkedBlockingQueue
	public void run() {
		InputStreamReader is;
		PrintStream os;
		Socket clientSocket = null;

        try {
			listener = new ServerSocket(listenPort);
		}
		catch (IOException e) {
			System.err.println("Error: Failed to instantiate the OrcX HTTP Server for send/receive, caught exception: ");
			System.err.println(e);
		}
		
		try {
			clientSocket = listener.accept();
			is = new InputStreamReader(clientSocket.getInputStream());
			os = new PrintStream(clientSocket.getOutputStream());

			while (!listener.isClosed()) {
				BufferedReader rdr = new BufferedReader(is);
				String line;

				// Read transmitted data and process HTTP headers, content, etc.
				// First line is the connection request
				line = rdr.readLine();
				if (!line.matches("\\s*POST \\S+ HTTP/\\S+")) {
					System.err.println("Warning: Malformed POST request sent to Orc server on port: " + listenPort);
				}
				boolean headers_done = false;
				int content_length = 0;
				String content = "";
				while ((line = rdr.readLine()) != null) {
					System.err.print("Got line: '");
					for (int i=0; i < line.getBytes().length; i++) {
						System.err.print(line.getBytes()[i] + ",");
					}
					System.err.println("'");
					if (line.length() == 0) {
						System.err.println("Got blank line");
						headers_done = true;
					}

					if (!headers_done) {
						// Split the headers by the ":" and process as needed.						 
						// The only headers we care about are the connection header and the content length
						String[] keyVal = line.split(":");
						if (keyVal[0].matches("Content-Length")) {
							content_length = (new Integer(keyVal[1].trim())).intValue();
						}
					}
					else {
						content += line;
						System.err.println("Content so far: " + content);
					}
					if (content.length() >= content_length) {
						break;
					}
				}
				if (content.length() != content_length) {
					System.err.println("Warning: Mismatch in content length from POST request.");
				}
				
				// Respond with "OK"
				os.println("HTTP/1.0 200 OK");
				os.println("Connection: close");
				os.println("Content-Type: text/html");  // necessary if we're not returning content?
				
				try {
					System.err.println("Queueing content: " + content);
					queue.put(content);
				}
				catch (Exception e) {
					System.err.println("Error: Attempted to write to OrcX send/receive queue, received exception:");
					System.err.println(e);
				}
			}
		}   
		catch (IOException e) {
			System.err.println(e);
		}
	}
}
