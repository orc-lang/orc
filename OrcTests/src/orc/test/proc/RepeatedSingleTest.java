//
// RepeatedSingleTest.java -- Java class RepeatedSingleTest
// Project OrcTests
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.proc;

import java.nio.file.Path;
import java.nio.file.Paths;

import orc.script.OrcBindings;
import orc.test.util.ExpectedOutput;
import orc.test.util.TestUtils;
import orc.test.util.TestUtils.OrcTestCase;

import junit.extensions.RepeatedTest;
import junit.framework.Test;

/**
 * Repeatedly run the test specified as a path in the TEST environment variable.
 * You can setup the Eclipse run configuration to set this variable to the
 * currently selected file. This makes for easy stress testing.
 *
 * @see ExamplesTest
 * @author amp
 */
public class RepeatedSingleTest {

    public static Test suite() {
        final Test suite = TestUtils.buildSuite(RepeatedSingleTest.class.getSimpleName(), (s, t, f, e, b) -> new RepeatedSingleTestCase(s, t, f, e, b), new OrcBindings(), Paths.get(System.getenv("TEST")));
        return new RepeatedTest(suite, 500);
    }

    public static class RepeatedSingleTestCase extends OrcTestCase {
        public RepeatedSingleTestCase(final String suiteName1, final String testName, final Path orcFile1, final ExpectedOutput expecteds1, final OrcBindings bindings1) {
            super(suiteName1, testName, orcFile1, expecteds1, bindings1);
            // TODO Auto-generated constructor stub
        }
        /* No overrides */
    }
}
