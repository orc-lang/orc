//
// ExamplesTest.java -- Java class ExamplesTest
// Project OrcTests
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.proc;

import java.io.File;

import orc.script.OrcBindings;
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
        final Test t = TestUtils.buildSuite(RepeatedSingleTest.class.getSimpleName(), ExamplesTestCase.class, new OrcBindings(), new File(System.getenv("TEST")));
        return new RepeatedTest(t, 500);
    }

    public static class ExamplesTestCase extends OrcTestCase {
        /* No overrides */
    }
}
