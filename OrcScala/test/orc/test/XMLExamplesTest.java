//
// XMLExamplesTest.java -- Java class XMLExamplesTest
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
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.concurrent.TimeoutException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import orc.error.OrcException;
import orc.ast.oil.nameless.Expression;
import orc.ast.oil.nameless.OrcXML;
import orc.script.OrcScriptEngine;

/**
 * Test Orc by running annotated sample programs from the "examples" directory.
 * Each program is compiled, written to XML, then read back as an AST. This
 * second AST is run. Each program is given at most 10 seconds to complete.
 * <p>
 * We look for one or more comment blocks formatted per
 * <code>ExampleOutput</code>'s specs.
 * 
 * @see ExpectedOutput
 * @author quark, srosario, amshali
 */
public class XMLExamplesTest {
	public static Test suite() {
		return buildSuite();
	}

	public static TestSuite buildSuite() {
		final TestSuite suite = new TestSuite("orc.test.XMLExamplesTest");
		final LinkedList<File> files = new LinkedList<File>();
		TestUtils.findOrcFiles(new File("examples"), files);
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
				public void runTest() throws InterruptedException, IOException, TimeoutException, OrcException, ClassNotFoundException, SecurityException, NoSuchFieldException, IllegalAccessException {
					System.out.println("\n==== Starting " + file + " ====");
					final OrcScriptEngine.OrcCompiledScript compiledScript = OrcForTesting.compile(file.getPath());
					final Expression expr = getAstRoot(compiledScript);
					final Expression exprXML = orc.ast.oil.nameless.OrcXML.fromXML(OrcXML.writeXML(expr));
					setAstRoot(compiledScript, exprXML);
					final String actual = OrcForTesting.run(compiledScript, 10L);
					if (!expecteds.contains(actual)) {
						throw new AssertionError("Unexpected output:\n" + actual);
					}
				}
			});
		}
		return suite;
	}

	static Expression getAstRoot(final OrcScriptEngine.OrcCompiledScript compiledScript) throws SecurityException, NoSuchFieldException, IllegalAccessException {
		// Violate access controls of OrcCompiledScript.astRoot field
		final Field astRootField = compiledScript.getClass().getDeclaredField("astRoot");
		astRootField.setAccessible(true);
		return (Expression) astRootField.get(compiledScript);
	}

	static void setAstRoot(final OrcScriptEngine.OrcCompiledScript compiledScript, final Expression astRoot) throws SecurityException, NoSuchFieldException, IllegalAccessException {
		// Violate access controls of OrcCompiledScript.astRoot field
		final Field astRootField = compiledScript.getClass().getDeclaredField("astRoot");
		astRootField.setAccessible(true);
		astRootField.set(compiledScript, astRoot);
	}

}
