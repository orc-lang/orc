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
            @SuppressWarnings("unchecked")
            final OrcScriptEngine<Object> engine = (OrcScriptEngine<Object>) compiledScript.getEngine();
            
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            engine.save(compiledScript, out);
            out.close();
            byte[] data = out.toByteArray();
            System.out.println("Serialized size = " + data.length + " bytes");
            
            final OrcScriptEngine<Object>.OrcCompiledScript compiledScriptAfter = engine.loadDirectly(new ByteArrayInputStream(data));

            // Execution
            final String actual = OrcForTesting.run(compiledScriptAfter, TESTING_TIMEOUT);
            if (!expecteds.contains(actual)) {
                throw new AssertionError("Unexpected output:\n" + actual);
            }
        }
    }
}
