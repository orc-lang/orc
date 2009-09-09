//
// Orc.java -- Java class Orc
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

package orc;

import java.io.IOException;
import java.io.Reader;

import orc.ast.extended.declaration.Declaration;
import orc.ast.extended.expression.Declare;
import orc.ast.oil.Compiler;
import orc.ast.oil.SiteResolver;
import orc.ast.oil.UnguardedRecursionChecker;
import orc.ast.oil.expression.Expression;
import orc.ast.simple.argument.Variable;
import orc.ast.xml.Oil;
import orc.env.Env;
import orc.error.compiletime.CompilationException;
import orc.parser.OrcParser;
import orc.progress.NullProgressListener;
import orc.progress.ProgressListener;
import orc.error.compiletime.CompileMessageRecorder.Severity;
import orc.runtime.OrcEngine;
import orc.runtime.nodes.Node;

/**
 * Main class for Orc. Parses Orc file and executes it.
 * <p>
 * Run with the argument "-help" to get a list of command-line options.
 * 
 * @author wcook, dkitchin
 */
public class Orc {

	/**
	 * Orc toplevel main function. Command line arguments are forwarded to Config for parsing.
	 * 
	 * @param args command-line arguments
	 */
	public static void main(final String[] args) {

		// Read configuration options from the environment and the command line
		final Config cfg = new Config();
		cfg.processArgs(args);

		final OrcCompiler compiler = new OrcCompiler(cfg);
		Expression ex;
		try {
			ex = compiler.call();
		} catch (final IOException e) {
			cfg.getMessageRecorder().recordMessage(Severity.FATAL, 0, e.getLocalizedMessage(), null, null, e);
			return;
		}

		if (ex == null) {
			return;
		}

		final Node n = orc.ast.oil.Compiler.compile(ex);

		// Configure the runtime engine
		final OrcEngine engine = new OrcEngine(cfg);

		// Run the Orc program
		engine.run(n);
	}

	public static Expression compile(final Reader source, final Config cfg) throws IOException {
		final OrcCompiler compiler = new OrcCompiler(cfg);
		return compiler.call();
	}
}
