package orc.orcx;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import orc.runtime.values.Constant;
import orc.runtime.values.Value;

import java.io.*;
import java.net.*;


/**
 * 
 * Centralized class for handling various aspects of the embedding of XQuery into Orc.
 * 
 * This is a prototype of the strategy that will be used to embed arbitrary companion
 * languages into Orc. Eventually, an execution of the Orc compiler and runtime will have
 * a mapping from embedded language names to objects that implement an interface derived from
 * this class's functionality.
 * 
 * @author dkitchin, kmorton
 */

public class OrcX {
	public OrcX() {
		
		/* One OrcX object is constructed to handle all embedded code.
		 * The constructor should take any information needed to connect to a galax process,
		 * request parsing, etc.
		 */
		
		// Set up the HTTP server for send/receive.
		// TODO: parameterize the port number
		OrcHTTPServer server = OrcHTTPServer.getServer();  // Launches the server in a singleton class
		Thread serverThread = new Thread(server);
		serverThread.start();
	}
	
	public static void terminate() {
		OrcHTTPServer.terminate();
	}
	
	
	/**
	 * 
	 * Send an XQuery in string form to Galax for parsing, retrieve an ID and a set of free
	 * variables for that query, and create an AST node containing that data.
	 * 
	 * @param s A string representation of the query 
	 * @return An Orc AST node enclosing the ID and free vars for the parsed query
	 */
	public orc.ast.extended.Expression embed(String s) {
		
		List<String> freevars = new ArrayList<String>();
		int queryid = 0;
		
		/* TODO Kristi: implement this */
		//strip off external braces from string s
		String substring = s.substring(1, s.length() -1);
		System.err.println("Embedding XQuery: " + substring);
		String results = HTTPUtils.send_galax("http://localhost:3001", HTTPUtils.GalaxCommand.compile_query, substring);
		//TODO later: String results2 = HTTPUtils.send_galax("http://localhost:3001", HTTPUtils.GalaxCommand.xml_to_string, (new Integer(10)).toString());
		String[] result_array = results.split("\n");
		
		//System.err.println("First item: "+ result_array[0]);
		queryid = new Integer(result_array[0]);
		
		for(int i = 1; i < result_array.length; i++){
			//System.err.println("Next item: " + result_array[i]);
			freevars.add(result_array[i]);
		}
		//System.err.println("Results: " + results);
        //System.exit(0);
		//reminder: this will change to EmbeddedXQuery(this, freevars)  
		return new EmbeddedXQuery(this, queryid, freevars);
	}

	/**
	 * 
	 * Execute the XQuery referenced by the query id, using the given set of
	 * values for its free variables.
	 * 
	 * @param queryid The integer ID for the query to run
	 * @param vals Values for each of the free variables in the query
	 * @return the value returned by the query evaluation
	 */
	public Value apply(int queryid, List<Value> vals) {
		
		Value ret = Value.signal();
		
		/* TODO Kristi: implement this */
		String execute_this = new String(queryid + "\n");
		for(Iterator i = vals.iterator(); i.hasNext(); )
			execute_this += i.next() + "\n";
		String results = HTTPUtils.send_galax("http://localhost:3001", HTTPUtils.GalaxCommand.execute_query, execute_this );
		return new Constant(results);
	}
	
}
