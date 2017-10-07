//
// DistribScaleTest.java -- Java class DistribScaleTest
// Project OrcTests
//
// Created by jthywiss on Oct 5, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.proc;

import junit.framework.Test;

/**
 * JUnit test suite for distributed-Orc scaling tests.
 *
 * @author jthywiss
 */
public class DistribScaleTest {

    /*
     * This TestSuite factory is in Java to keep JUnit from being confused by
     * Scala.
     */
    public static Test suite() {
        return DistribScaleTestCase.buildTestSuite();
    }

}
