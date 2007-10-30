package orc.orcx;

import java.util.LinkedList;
import java.util.List;


import orc.parser.OrcLexer;
import orc.runtime.values.Constant;
import orc.runtime.values.Value;

import org.exist.storage.DBBroker;
import org.exist.xquery.AnalyzeContextInfo;
import org.exist.xquery.PathExpr;
import org.exist.xquery.StaticXQueryException;
import org.exist.xquery.XPathException;
import org.exist.xquery.parser.XQueryLexer;
import org.exist.xquery.parser.XQueryParser;
import org.exist.xquery.parser.XQueryTreeParser;
import org.exist.xquery.value.Item;
import org.exist.xquery.value.NodeValue;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.SequenceIterator;
import org.exist.xquery.value.StringValue;

import antlr.RecognitionException;
import antlr.TokenStreamException;
import antlr.collections.AST;

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
		
		
		
		/* TODO Kristi: implement this */
		return new EmbeddedXQuery()
	}
	

	/**
	 * Convert an XPath sequence into a list of Orc values
	 * @param s a sequence as defined in the XPath semantics
	 * @return A list of Orc values
	 * @throws OrcXException 
	 */
	public static List<Value> convertToValues(Sequence s) throws OrcXException {
		
		SequenceIterator items = s.unorderedIterator();
		List<Value> values = new LinkedList<Value>();
		
		while(items.hasNext()) {
			Item i = items.nextItem();
			values.add(convertToValue(i));
		}
		
		return values;
	}
	

	public static Value convertToValue(Item item) throws OrcXException {
		
		Value result;
		
		if (item instanceof NodeValue) {
			result = new XMLNode((NodeValue)item);
		}
		else {
			try {
				result = new Constant(item.getStringValue());
			} catch (XPathException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new OrcXException();
			}
		}
		
		return result;
	}
	

	public static Sequence convertToSequence(Value val) throws OrcXException {
		
		Item result;
		
		if (val instanceof Constant) {
			Constant c = (Constant)val;
			result = new StringValue(c.getValue().toString());
		}
		else if (val instanceof XMLNode) {
			XMLNode x = (XMLNode)val;
			result = x.getNode();
		}
		else {
			throw new OrcXException();
		}
		
		return result.toSequence();
	}
	
}
