//
// RepeatExampleTests.java -- Java class RepeatExampleTests
// Project OrcTests
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test;

import junit.extensions.RepeatedTest;
import junit.framework.Test;

/**
 * Repeatedly run the ExamplesTest suite.
 *
 * @author jthywiss
 */
public class RepeatExampleTests {
    public static Test suite() {
        return new RepeatedTest(ExamplesTest.suite(), 500);
    }
}
