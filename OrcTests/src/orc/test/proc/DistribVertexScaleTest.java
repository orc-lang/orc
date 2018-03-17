//
// DistribVertexScaleTest.java -- Java class DistribVertexScaleTest
// Project OrcTests
//
// Created by jthywiss on Mar 17, 2018.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.proc;

import junit.framework.Test;

/**
 * JUnit test suite for distributed-Orc vertex scaling tests.
 *
 * @author jthywiss
 */
public class DistribVertexScaleTest {

    /*
     * This TestSuite factory is in Java to keep JUnit from being confused by
     * Scala.
     */
    public static Test suite() {
        return DistribVertexScaleTestCase.buildTestSuite();
    }

}
