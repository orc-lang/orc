package orc.parser;

import orc.error.compiletime.ParsingException;
import xtc.parser.ParseException;

/**
 * Signal an unrecoverable error during parsing. For example, a parse
 * error inside an include file is not recoverable because there is only
 * one way to parse an include file. This must be an unchecked
 * exception because Rats does not allow us to throw any checked exceptions.
 * 
 * @author quark
 */
public final class AbortParse extends RuntimeException {
	public AbortParse(String message) {
		super(message);
	}
}
