//
// XMLExamplesTest.java -- Java class XMLExamplesTest
// Project OrcTests
//
// $Id$
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
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
import orc.ast.oil.xml.OrcXML;
import orc.script.OrcScriptEngine;
import orc.script.OrcBindings;

import javax.xml.validation.*;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.SAXException;

/**
 * Test Orc by running annotated sample programs from the "../OrcExamples" directory.
 * Each program is compiled, written to XML, subjected to validation against
 * an XML schema, and then read back as an AST. This second AST is run. 
 * Each program is given at most 10 seconds to complete.
 * <p>
 * We look for one or more comment blocks formatted per
 * <code>ExampleOutput</code>'s specs.
 * 
 * @see ExpectedOutput
 * @author quark, srosario, amshali, dkitchin
 */
public class XMLExamplesTest {
	public static Test suite() {
		return buildSuite();
	}

	public static TestSuite buildSuite() {
		final TestSuite suite = new TestSuite("orc.test.XMLExamplesTest");
		final LinkedList<File> files = new LinkedList<File>();
        TestUtils.findOrcFiles(new File("test_data"), files);
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
				public void runTest() throws InterruptedException, IOException, TimeoutException, OrcException, ClassNotFoundException, SecurityException, NoSuchFieldException, IllegalAccessException {
					System.out.println("\n==== Starting " + file + " ====");
					final OrcScriptEngine.OrcCompiledScript compiledScript = OrcForTesting.compile(file.getPath(), new OrcBindings());
					final Expression expr = getAstRoot(compiledScript);
					
					// AST -> XML
					final scala.xml.Elem xmlFromExpr = OrcXML.astToXml(expr);
					
					 
					// Locate .xsd file resource
					final ClassLoader clX = Thread.currentThread().getContextClassLoader();
					final ClassLoader clY = getClass().getClassLoader();
					final ClassLoader clZ = ClassLoader.getSystemClassLoader();
					final ClassLoader classLoader = clX != null ? clX : (clY != null ? clY : clZ);
					final java.io.InputStream xsdStream = classLoader.getResource("orc/ast/oil/xml/oil.xsd").openStream();
					
					// Schema validation
					try {
				      final SchemaFactory schemaFactory = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
	                  final Schema schema = schemaFactory.newSchema(new StreamSource(xsdStream));
	                  final Validator validator = schema.newValidator();
	                  final StreamSource xmlsource = new StreamSource(new java.io.StringReader(xmlFromExpr.toString()));
	                  validator.validate(xmlsource);
                    } 
				    catch (SAXException e) {
				      throw new AssertionError("XML validation failed: " + e.getMessage());
                    }
									    
					// XML -> AST
					final Expression exprFromXml = OrcXML.xmlToAst(xmlFromExpr);
					
					// Execution
					setAstRoot(compiledScript, exprFromXml);
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
