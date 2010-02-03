//
// AbstractCompilerService.java -- Java class AbstractCompilerService
// Project Orchard
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.orchard;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Logger;

import orc.Config;
import orc.Orc;
import orc.ast.xml.Oil;
import orc.error.compiletime.CompilationException;
import orc.orchard.errors.InvalidProgramException;

/**
 * Implementation of a compiler service.
 * @author quark
 */
public abstract class AbstractCompilerService implements orc.orchard.api.CompilerServiceInterface {
	protected Logger logger;

	protected AbstractCompilerService(final Logger logger) {
		this.logger = logger;
	}

	protected AbstractCompilerService() {
		this(getDefaultLogger());
	}

	public Oil compile(final String devKey, final String program) throws InvalidProgramException {
		logger.info("compile(" + devKey + ", " + program + ")");
		if (program == null) {
			throw new InvalidProgramException("Null program!");
		}
		try {
			final Config config = new Config();
			// Disable file resources for includes
			config.setIncludePath("");
			// Include sites specifically for orchard services
			config.addInclude("orchard.inc");
			config.inputFromString(program);
			final ByteArrayOutputStream ourStdErrBytes = new ByteArrayOutputStream();
			config.setStderr(new PrintStream(ourStdErrBytes));
			final orc.ast.oil.expression.Expression ex1 = Orc.compile(config);
			if (ex1 == null) {
				throw new InvalidProgramException(ourStdErrBytes.toString());
			}
			return new Oil("1.0", ex1.marshal());
		} catch (final CompilationException e) {
			throw new InvalidProgramException(e.getMessage());
		} catch (final IOException e) {
			throw new InvalidProgramException("IO error: " + e.getMessage());
		}
	}

	protected static Logger getDefaultLogger() {
		final Logger out = Logger.getLogger(AbstractExecutorService.class.toString());
		return out;
	}
}
