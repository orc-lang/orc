package orc.parser;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import orc.Config;
import orc.ast.extended.declaration.Declaration;
import orc.ast.extended.expression.Expression;
import orc.error.SourceLocation;
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
	public OrcParser(Config config, Reader reader) {
		this(config, reader, "<stdin>");
	}
	/**
	 * If you know the filename, it can be used to improve
	 * parse error messages.
	 */
	public OrcParser(Config config, Reader reader, String filename) {
		parser = new OrcParserRats(config, reader, filename);
	}
	
	/**
	 * Parse the input as a complete program (declarations plus goal
	 * expression).
	 */
	public Expression parseProgram() throws ParsingException, IOException {
		Result result = null;
		try {
			result = parser.pProgram(0);
			return (Expression)parser.value(result);
		} catch (AbortParse e) {
			ParsingException pe = new ParsingException(e.parseError.msg, e);
			parser.at(e.parseError.index, e.parseError.index+2, pe);
			throw pe;
		} catch (ParseException e) {
			if (result != null) {
				ParsingException pe = new ParsingException(result.parseError().msg, e);
				parser.at(result.parseError().index, result.parseError().index+2, pe);
				throw pe;
			}
			// We shouldn't be here, but just in case...
			throw new ParsingException(e.getMessage());
		}
	}
	
	/**
	 * Parse the input as a module (declarations only).
	 */
	public List<Declaration> parseModule() throws ParsingException, IOException {
		Result result = null;
		try {
			result = parser.pModule(0);
			return (List<Declaration>)parser.value(result);
		} catch (AbortParse e) {
			ParsingException pe = new ParsingException(e.parseError.msg, e);
			parser.at(e.parseError.index, e.parseError.index+2, pe);
			throw pe;
		} catch (ParseException e) {
			if (result != null) {
				ParsingException pe = new ParsingException(result.parseError().msg, e);
				parser.at(result.parseError().index, result.parseError().index+2, pe);
				throw pe;
			}
			// We shouldn't be here, but just in case...
			throw new ParsingException(e.getMessage());
		}
	}
	
	/**
	 * For testing purposes; parses a program from stdin or a file given as an
	 * argument, and prints the parsed program.
	 */
	public static void main(String[] args) throws IOException, ParsingException {
		Config cfg = new Config();
		cfg.processArgs(args);	
		OrcParser p = new OrcParser(cfg, cfg.getReader(), cfg.getInputFilename());
		System.out.println(p.parseProgram().toString());
	}
}
