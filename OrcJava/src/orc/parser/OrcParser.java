//
// OrcParser.java -- Java class OrcParser
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.parser;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

import orc.Config;
import orc.ast.extended.declaration.Declaration;
import orc.ast.extended.expression.Expression;
import orc.error.compiletime.ParsingException;
import xtc.parser.ParseException;
import xtc.parser.Result;

/**
 * Interface to the parser. This isolates other code from
 * the underlying parser technology.
 * 
 * @author quark
 */
public class OrcParser {
	private final OrcParserRats parser;

	public OrcParser(final Config config, final Reader reader) {
		this(config, reader, "<stdin>");
	}

	/**
	 * If you know the filename, it can be used to improve
	 * parse error messages.
	 */
	public OrcParser(final Config config, final Reader reader, final String filename) {
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
			return (Expression) parser.value(result);
		} catch (final AbortParse e) {
			final ParsingException pe = new ParsingException(e.parseError.msg, e);
			parser.at(e.parseError.index, e.parseError.index + 2, pe);
			throw pe;
		} catch (final ParseException e) {
			if (result != null) {
				final ParsingException pe = new ParsingException(result.parseError().msg, e);
				parser.at(result.parseError().index, result.parseError().index + 2, pe);
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
			return (List<Declaration>) parser.value(result);
		} catch (final AbortParse e) {
			final ParsingException pe = new ParsingException(e.parseError.msg, e);
			parser.at(e.parseError.index, e.parseError.index + 2, pe);
			throw pe;
		} catch (final ParseException e) {
			if (result != null) {
				final ParsingException pe = new ParsingException(result.parseError().msg, e);
				parser.at(result.parseError().index, result.parseError().index + 2, pe);
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
	public static void main(final String[] args) throws IOException, ParsingException {
		final Config cfg = new Config();
		cfg.processArgs(args);
		final OrcParser p = new OrcParser(cfg, cfg.getReader(), cfg.getInputFilename());
		System.out.println(p.parseProgram().toString());
	}
}
