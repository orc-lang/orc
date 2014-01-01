//
// TypedExamplesTest.java -- Java class TypedExamplesTest
// Project OrcTests
//
// $Id: TypedExamplesTest.java 3261 2013-09-08 14:02:44Z jthywissen $
//
// Created by dkitchin on Mar 30, 2011.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.concurrent.TimeoutException;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import junit.framework.Test;
import junit.framework.TestSuite;
import orc.PorcInterpreterBackend;
import orc.error.OrcException;
import orc.script.OrcBindings;
import orc.script.OrcScriptEngine.OrcCompiledScript;
import orc.test.TestUtils.OrcTestCase;

import static org.junit.Assume.assumeNoException;

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
@RunWith(Suite.class)
@Suite.SuiteClasses({ PorcExamplesTest.PorcUnoptExamplesTest.class, PorcExamplesTest.PorcOptimizedExamplesTest.class })
public class PorcExamplesTest {
  public static class PorcOrcTestCase extends OrcTestCase {
    /**
     * Compiler failures that are related to VTime result in ignores.
     * @see orc.test.TestUtils.OrcTestCase#compileAndRun()
     */
    @Override
    protected String compileAndRun() throws ClassNotFoundException, FileNotFoundException, OrcException, TimeoutException {
      // TODO Auto-generated method stub
      String filename = orcFile.getPath();
      OrcCompiledScript code;
      try {
        code = OrcForTesting.compile(filename, bindings);
      } catch (IllegalArgumentException e) {
        assumeNoException(e);
        throw new Error(e);
      }
      return OrcForTesting.run(code, TESTING_TIMEOUT);
    }
  }
  
  
  public static class PorcOptimizedExamplesTest {
    public static Test suite() {
      OrcBindings optbindings = new OrcBindings();
      optbindings.backend_$eq(PorcInterpreterBackend.it());
      optbindings.optimizationLevel_$eq(3);

      return TestUtils.buildSuite(PorcOptimizedExamplesTest.class.getSimpleName(), ExamplesTestCase.class, optbindings, new File("test_data"), new File("../OrcExamples"));
    }
    public static class ExamplesTestCase extends PorcOrcTestCase {
      /* No overrides */
    }
  }

  public static class PorcUnoptExamplesTest {
    public static Test suite() {
      OrcBindings bindings = new OrcBindings();
      bindings.backend_$eq(PorcInterpreterBackend.it());

      return TestUtils.buildSuite(PorcUnoptExamplesTest.class.getSimpleName(), ExamplesTestCase.class, bindings, new File("test_data"), new File("../OrcExamples"));
    }
    public static class ExamplesTestCase extends PorcOrcTestCase {
      /* No overrides */
    }
  }
}
