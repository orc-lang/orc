//
// ExamplesTest.java -- Java class ExamplesTest
// Project OrcTests
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.proc;

import java.io.File;

import orc.script.OrcBindings;
import orc.test.util.ExpectedOutput;
import orc.test.util.TestUtils;
import orc.test.util.TestUtils.OrcTestCase;

import junit.framework.Test;

/**
 * Test Orc by running annotated sample programs from the "../OrcExamples"
 * directory. Each program is given at most
 * TestUtils.OrcTestCase.TESTING_TIMEOUT seconds to complete.
 * <p>
 * We look for one or more comment blocks formatted per
 * <code>ExampleOutput</code>'s specs.
 *
 * @see ExpectedOutput
 * @author quark, srosario
 */
public class ExamplesTest {

    public static Test suite() {
        return TestUtils.buildSuite(ExamplesTest.class.getSimpleName(), (s, t, f, e, b) -> new ExamplesTestCase(s, t, f, e, b), new OrcBindings(), new File("test_data"), new File("../OrcExamples"));
    }

    public static class ExamplesTestCase extends OrcTestCase {
        public ExamplesTestCase(final String suiteName1, final String testName, final File orcFile1, final ExpectedOutput expecteds1, final OrcBindings bindings1) {
            super(suiteName1, testName, orcFile1, expecteds1, bindings1);
        }
        /* No overrides */
    }
}
