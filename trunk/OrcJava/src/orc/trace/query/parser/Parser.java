package orc.trace.query.parser;

import java.io.IOException;
import java.io.Reader;

import orc.error.compiletime.ParsingException;
import orc.trace.query.predicates.Predicate;
import xtc.parser.ParseException;
import xtc.parser.Result;

/**
 * Provide a friendlier interface to the Rats generated parser.
 * @author quark
 */
public class Parser {
	private ParserRats parser;
	public Parser(Reader reader) {
		this(reader, "<stdin>");
	}
	
	/**
	 * If you know the filename, it can be used to improve
	 * parse error messages.
	 */
	public Parser(Reader reader, String filename) {
		parser = new ParserRats(reader, filename);
	}
	
	/**
	 * Parse the input as a standalone query.
	 */
	public Predicate parseQuery() throws ParsingException, IOException {
		Result result = parser.pQuery(0);
		try {
			return (Predicate)parser.value(result);
		} catch (ParseException e) {
			throw new ParsingException(e.getMessage(), e);
		}
	}
}
