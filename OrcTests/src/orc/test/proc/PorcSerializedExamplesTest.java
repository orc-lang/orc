//
// PorcSerializedExamplesTest.java -- Java class PorcSerializedExamplesTest
// Project OrcTests
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.proc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import orc.script.OrcBindings;
import orc.script.OrcScriptEngine;
import orc.test.item.OrcForTesting;
import orc.test.util.ExpectedOutput;
import orc.test.util.TestUtils;
import orc.test.util.TestUtils.OrcTestCase;

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
        final OrcBindings bindings = new OrcBindings();

        // Turn on Orctimizer
        bindings.backend_$eq(orc.BackendType.fromString("porc"));
        bindings.optimizationLevel_$eq(2);

        return TestUtils.buildSuite(PorcSerializedExamplesTest.class.getSimpleName(), (s, t, f, e, b) -> new PorcSerializedExamplesTestCase(s, t, f, e, b), bindings, Paths.get("test_data"), Paths.get("../OrcExamples"));
    }

    public static class PorcSerializedExamplesTestCase extends OrcTestCase {
        public PorcSerializedExamplesTestCase(final String suiteName1, final String testName, final Path orcFile1, final ExpectedOutput expecteds1, final OrcBindings bindings1) {
            super(suiteName1, testName, orcFile1, expecteds1, bindings1);
        }

        @Override
        public void runTest() throws Throwable {
            System.out.println("\n==== Starting " + orcFile + " ====");
            final OrcScriptEngine<Object>.OrcCompiledScript compiledScript = OrcForTesting.compile(orcFile.toString(), bindings);
            @SuppressWarnings("unchecked")
            final OrcScriptEngine<Object> engine = (OrcScriptEngine<Object>) compiledScript.getEngine();

            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            engine.save(compiledScript, out);
            out.close();
            final byte[] data = out.toByteArray();
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
