//
// Orc.java -- Java class Orc
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc;

import java.io.IOException;

import orc.ast.oil.expression.Expression;
import orc.error.compiletime.CompileMessageRecorder.Severity;
import orc.runtime.OrcEngine;

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

		
		if (cfg.noExecute()) {
			System.out.println("Execution suppressed by -noexecute switch.");
		}
		else {
			// Configure the runtime engine
			final OrcEngine engine = new OrcEngine(cfg);

			// Run the Orc program
			engine.run(ex);
		}
	}

	public static Expression compile(final Config cfg) throws IOException {
		final OrcCompiler compiler = new OrcCompiler(cfg);
		return compiler.call();
	}
}
