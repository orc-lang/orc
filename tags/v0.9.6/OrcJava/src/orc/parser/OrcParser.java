package orc.parser;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import orc.ast.extended.Expression;
import orc.ast.extended.declaration.Declaration;
import orc.error.compiletime.ParsingException;
import orc.lib.str.Read;
import xtc.parser.ParseException;
import xtc.parser.Result;

/**
 * Interface to the parser. This isolates other code from
 * the underlying parser technology.
 * 
 * @author quark
 */
public class OrcParser {
	private OrcParserRats parser;
	public OrcParser(Reader reader) {
		this(reader, "<stdin>");
	}
	/**
	 * If you know the filename, it can be used to improve
	 * parse error messages.
	 */
	public OrcParser(Reader reader, String filename) {
		parser = new OrcParserRats(reader, filename);
	}
	
	/**
	 * Parse the input as a literal value. This
	 * is used by {@link Read}.
	 */
	public Object parseLiteralValue() throws ParsingException, IOException {
		Result result = parser.pLiteralValue(0);
		try {
			return parser.value(result);
		} catch (ParseException e) {
			throw new ParsingException(e.getMessage(), e);
		}
	}
	
	/**
	 * Parse the input as a complete program (declarations plus goal
	 * expression).
	 */
	public Expression parseProgram() throws ParsingException, IOException {
		Result result = parser.pProgram(0);
		try {
			return (Expression)parser.value(result);
		} catch (ParseException e) {
			throw new ParsingException(e.getMessage(), e);
		}
	}
	
	/**
	 * Parse the input as a module (declarations only).
	 */
	public List<Declaration> parseModule() throws ParsingException, IOException {
		Result result = parser.pModule(0);
		try {
			return (List<Declaration>)parser.value(result);
		} catch (ParseException e) {
			throw new ParsingException(e.getMessage());
		}
	}
	
	/**
	 * For testing purposes; parses a program from stdin or a file given as an
	 * argument, and prints the parsed program.
	 */
	public static void main(String[] args) throws IOException, ParsingException {
		Reader r;
		if (args.length > 0) {
			r = new FileReader(args[0]);
		} else {
			r = new InputStreamReader(System.in); 
		}
		OrcParser p = new OrcParser(r);
		System.out.println(p.parseProgram().toString());
	}
}
