//
// TypedExamplesTest.java -- Java class TypedExamplesTest
// Project OrcTests
//
// Created by dkitchin on Mar 30, 2011.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test;

import java.io.File;

import orc.script.OrcBindings;
import orc.test.TestUtils.OrcTestCase;

import junit.framework.Test;

/**
 * Test Orc by running annotated sample programs from the "../OrcExamples"
 * directory. The Orc type checker is enabled for these tests. Each program is
 * given at most 10 seconds to complete.
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

        return TestUtils.buildSuite(TypedExamplesTest.class.getSimpleName(), TypedExamplesTestCase.class, bindings, new File("test_data"), new File("../OrcExamples"));
    }

    public static class TypedExamplesTestCase extends OrcTestCase {
        /* No overrides */
    }

}
