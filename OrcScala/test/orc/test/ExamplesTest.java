//
// ExamplesTest.java -- Java class ExamplesTest
// Project OrcScala
//
// $Id$
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.TimeoutException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import orc.error.OrcException;
import orc.error.compiletime.CompilationException;

import orc.script.OrcBindings;

/**
 * Test Orc by running annotated sample programs from the "../OrcExamples" directory.
 * Each program is given at most 10 seconds to complete.
 * <p>
 * We look for one or more comment blocks formatted per
 * <code>ExampleOutput</code>'s specs.
 * 
 * @see ExpectedOutput
 * @author quark, srosario
 */
public class ExamplesTest {
  
	public static Test suite() {
		return buildSuite(new OrcBindings());
	}

	public static TestSuite buildSuite(final OrcBindings bindings) {
		final TestSuite suite = new TestSuite("orc.test.ExamplesTest");
		final LinkedList<File> files = new LinkedList<File>();
		TestUtils.findOrcFiles(new File("../OrcExamples"), files);
		for (final File file : files) {
			final ExpectedOutput expecteds;
			try {
				expecteds = new ExpectedOutput(file);
			} catch (final IOException e) {
				throw new AssertionError(e);
			}
			// skip tests with no expected output
			if (expecteds.isEmpty()) {
				continue;
			}
			suite.addTest(new TestCase(file.toString()) {
				@Override
				public void runTest() throws InterruptedException, IOException, TimeoutException, OrcException, ClassNotFoundException {
					System.out.println("\n==== Starting " + file + " ====");
					try {
					    final String actual = OrcForTesting.compileAndRun(file.getPath(), 10L, bindings);
	                    if (!expecteds.contains(actual)) {
	                        throw new AssertionError("Unexpected output:\n" + actual);
	                    }
					} catch (CompilationException ce) {
					    throw new AssertionError(ce.getMessageAndDiagnostics());
					}
				}
			});
		}
		return suite;
	}
}
