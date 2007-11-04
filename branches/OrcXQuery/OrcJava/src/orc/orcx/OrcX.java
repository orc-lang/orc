package orc.orcx;

import java.util.ArrayList;
import java.util.List;

import orc.runtime.values.Constant;
import orc.runtime.values.Value;


/**
 * 
 * Centralized class for handling various aspects of the embedding of XQuery into Orc.
 * 
 * This is a prototype of the strategy that will be used to embed arbitrary companion
 * languages into Orc. Eventually, an execution of the Orc compiler and runtime will have
 * a mapping from embedded language names to objects that implement an interface derived from
 * this class's functionality.
 * 
 * @author dkitchin
 */

public class OrcX {
	
	
	public OrcX() {
		
		/* One OrcX object is constructed to handle all embedded code.
		 * The constructor should take any information needed to connect to a galax process,
		 * request parsing, etc.
		 */
		
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
		System.err.println("Embedding XQuery: " + s);
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
		
		return ret;
	}
	
}
