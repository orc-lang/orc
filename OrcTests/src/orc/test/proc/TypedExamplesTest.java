//
// TypedExamplesTest.java -- Java class TypedExamplesTest
// Project OrcTests
//
// Created by dkitchin on Mar 30, 2011.
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

import junit.framework.Test;

/**
 * Test Orc by running annotated sample programs from the "../OrcExamples"
 * directory. The Orc type checker is enabled for these tests. Each program is
 * given at most TestUtils.OrcTestCase.TESTING_TIMEOUT seconds to complete.
 * <p>
 * We look for one or more comment blocks formatted per
 * <code>ExampleOutput</code>'s specs.
 *
 * @see ExpectedOutput
 * @author dkitchin
 */
public class TypedExamplesTest {

    public static Test suite() {
        final OrcBindings bindings = new OrcBindings();

        // Turn on typechecking
        bindings.typecheck_$eq(true);

        return TestUtils.buildSuite(TypedExamplesTest.class.getSimpleName(), (s, t, f, e, b) -> new TypedExamplesTestCase(s, t, f, e, b), bindings, Paths.get("test_data"), Paths.get("../OrcExamples"));
    }

    public static class TypedExamplesTestCase extends OrcTestCase {
        public TypedExamplesTestCase(final String suiteName1, final String testName, final Path orcFile1, final ExpectedOutput expecteds1, final OrcBindings bindings1) {
            super(suiteName1, testName, orcFile1, expecteds1, bindings1);
        }
        /* No overrides */
    }

}
