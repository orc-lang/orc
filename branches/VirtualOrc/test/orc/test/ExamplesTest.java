//
// ExamplesTest.java -- Java class ExamplesTest
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

package orc.test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import orc.Config;
import orc.Orc;
import orc.error.compiletime.CompilationException;
import orc.runtime.OrcEngine;

import org.kohsuke.args4j.CmdLineException;

/**
 * Test Orc by running annotated sample programs from the "examples" directory.
 * Each program is given at most 10 seconds to complete.
 * 
 * <p>
 * We look for one or more comment blocks with a line starting with "OUTPUT:",
 * and take everything in the comment after the "OUTPUT:" line to be a possible
 * output of the program. Example:
 * 
 * <pre>
 * {-
 * OUTPUT:
 * 1
 * 2
 * -}
 * 1 | 2
 * </pre>
 * 
 * If none of the expected outputs match the actual output, the test fails for
 * that example. The multiple output blocks let us cope with limited
 * non-determinism, but a better solution is needed for serious testing of
 * non-deterministic programs.
 * 
 * @author quark
 */
public class ExamplesTest {
	public static Test suite() {
		final Config config = new Config();
		return buildSuite(config);
	}

	public static TestSuite buildSuite(final Config config) {
		final TestSuite suite = new TestSuite("orc.test.ExamplesTest");
		final LinkedList<File> files = new LinkedList<File>();
		TestUtils.findOrcFiles(new File("examples"), files);
		for (final File file : files) {
			final LinkedList<String> expecteds;
			try {
				expecteds = extractExpectedOutput(file);
			} catch (final IOException e) {
				throw new AssertionError(e);
			}
			// skip tests with no expected output
			if (expecteds.isEmpty()) {
				continue;
			}
			suite.addTest(new TestCase(file.toString()) {
				@Override
				public void runTest() throws IOException, CmdLineException, CompilationException, InterruptedException, Throwable, TimeoutException {
					runOrcProgram(config, file, expecteds);
				}
			});
		}
		return suite;
	}

	public static void runOrcProgram(final Config config, final File file, final LinkedList<String> expecteds) throws InterruptedException, Throwable, CmdLineException, CompilationException, IOException, TimeoutException {
		// configure engine to write to a ByteArray
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		config.setInputFile(file);
		config.setStdout(new PrintStream(out));
		config.setStderr(config.getStdout());

		/* 
		 * Shorten errors. 
		 * Backtraces may contain filesystem-specifc paths and other details,
		 * which will interfere with output matching. 
		 */
		config.setShortErrors(true);

		/*
		 * If the typechecker does run, silence its output.
		 */
		config.setQuietChecking(true);

		final OrcEngine engine = new OrcEngine(config);
		final orc.ast.oil.expression.Expression expr = Orc.compile(config);
		if (expr == null) {
			throw new CompilationException("Compilation to OIL failed");
		}
		engine.start(expr);

		// run the engine with a fixed timeout
		final FutureTask<?> future = new FutureTask<Void>(engine, null);
		new Thread(future).start();
		try {
			future.get(10L, TimeUnit.SECONDS);
		} catch (final TimeoutException e) {
			future.cancel(true);
			throw e;
		} catch (final ExecutionException e) {
			throw e.getCause();
		}

		// compare the output to the expected result
		final String actual = out.toString();
		for (final String expected : expecteds) {
			if (expected.equals(actual)) {
				return;
			}
		}
		throw new AssertionError("Unexpected output:\n" + actual);
	}

	public static LinkedList<String> extractExpectedOutput(final File file) throws IOException {
		final BufferedReader r = new BufferedReader(new FileReader(file));
		final LinkedList<String> out = new LinkedList<String>();
		StringBuilder oneOutput = null;
		for (String line = r.readLine(); line != null; line = r.readLine()) {
			if (oneOutput != null) {
				if (line.startsWith("-}")) {
					out.add(oneOutput.toString());
					oneOutput = null;
				} else {
					oneOutput.append(line);
					oneOutput.append("\n");
				}
			} else if (line.startsWith("OUTPUT:")) {
				oneOutput = new StringBuilder();
			}
		}
		return out;
	}
}
