//
// DistribTest.java -- Java class DistribTest
// Project OrcTests
//
// Created by jthywiss on Jul 29, 2017.
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

import junit.framework.Test;

/**
 * JUnit test suite for distributed-Orc tests.
 *
 * @author jthywiss
 */
public class DistribTest {

    /*
     * This TestSuite factory is in Java to keep JUnit from being confused by
     * Scala.
     */
    public static Test suite() {
        DistribTestCase.setUpTestSuite();
        final Path[] programPaths = { Paths.get("test_data/functional_valid/distrib") };
        return DistribTestCase.buildSuite(programPaths);
    }
}
