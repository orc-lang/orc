//
// ExamplesTest.java -- Java class ExamplesTest
// Project OrcTests
//
// $Id: ExamplesTest.java 3261 2013-09-08 14:02:44Z jthywissen $
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test;

import java.io.File;

import junit.extensions.RepeatedTest;
import junit.framework.Test;

import orc.script.OrcBindings;
import orc.test.TestUtils.OrcTestCase;

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
public class RepeatedSingleTest {

	public static Test suite() {
		Test t = TestUtils.buildSuite(RepeatedSingleTest.class.getSimpleName(), ExamplesTestCase.class, new OrcBindings(), new File(System.getenv("TEST")));
		return new RepeatedTest(t, 500);
	}

	public static class ExamplesTestCase extends OrcTestCase {
        /* No overrides */
    }
}
