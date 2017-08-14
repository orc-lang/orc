//
// PorcSerializedExamplesTest.java -- Java class PorcSerializedExamplesTest
// Project OrcTests
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.reflect.Field;

import orc.ast.oil.nameless.Expression;
import orc.script.OrcBindings;
import orc.script.OrcScriptEngine;
import orc.test.TestUtils.OrcTestCase;

import junit.framework.Test;

/**
 * Test Orc by running annotated sample programs from the "../OrcExamples"
 * directory.
 *
 * @see ExpectedOutput
 * @author quark, srosario, amshali, dkitchin, jthywiss, amp
 */
public class PorcSerializedExamplesTest {
    public static Test suite() {
      OrcBindings bindings = new OrcBindings();

      // Turn on Orctimizer
      bindings.backend_$eq(orc.BackendType.fromString("porc"));
      bindings.optimizationLevel_$eq(2);

      return TestUtils.buildSuite(PorcSerializedExamplesTest.class.getSimpleName(), PorcSerializedExamplesTestCase.class, bindings, new File("test_data"), new File("../OrcExamples"));
    }

    public static class PorcSerializedExamplesTestCase extends OrcTestCase {
        @Override
        public void runTest() throws Throwable {
            System.out.println("\n==== Starting " + orcFile + " ====");
            final OrcScriptEngine<Object>.OrcCompiledScript compiledScript = OrcForTesting.compile(orcFile.getPath(), bindings);
            final OrcScriptEngine<Object> engine = (OrcScriptEngine<Object>) compiledScript.getEngine();
            
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            engine.save(compiledScript, out);
            out.close();
            
            
            final OrcScriptEngine<Object>.OrcCompiledScript compiledScriptAfter = engine.loadDirectly(new ByteArrayInputStream(out.toByteArray()));

            // Execution
            final String actual = OrcForTesting.run(compiledScriptAfter, TESTING_TIMEOUT);
            if (!expecteds.contains(actual)) {
                throw new AssertionError("Unexpected output:\n" + actual);
            }
        }
    }

    static Expression getAstRoot(final OrcScriptEngine<Object>.OrcCompiledScript compiledScript) throws SecurityException, NoSuchFieldException, IllegalAccessException {
        // Violate access controls of OrcCompiledScript.astRoot field
        final Field codeField = compiledScript.getClass().getDeclaredField("code");
        codeField.setAccessible(true);
        return (Expression) codeField.get(compiledScript);
    }

    static void setAstRoot(final OrcScriptEngine<Object>.OrcCompiledScript compiledScript, final Expression astRoot) throws SecurityException, NoSuchFieldException, IllegalAccessException {
        // Violate access controls of OrcCompiledScript.astRoot field
        final Field codeField = compiledScript.getClass().getDeclaredField("code");
        codeField.setAccessible(true);
        codeField.set(compiledScript, astRoot);
    }

}
