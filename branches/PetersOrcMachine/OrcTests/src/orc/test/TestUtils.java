//
// TestUtils.java -- Java class TestUtils
// Project OrcTests
//
// $Id$
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import orc.error.compiletime.CompilationException;
import orc.script.OrcBindings;

public final class TestUtils {
	private TestUtils() {
		/* Only static members*/
	}

	abstract public static class OrcTestCase extends TestCase {
	    /**
	     * The timeout to use for testing. This provides a central location to change it.
	     */
	    public static final long TESTING_TIMEOUT = 20L;
	    
        protected String suiteName;
	    protected File orcFile;
	    protected ExpectedOutput expecteds;
	    protected OrcBindings bindings;

        OrcTestCase() {
	        super();
	    }

        void otcInit(String suiteName1, String testName, File orcFile1, ExpectedOutput expecteds1, OrcBindings bindings1) {
            setName(testName);
            this.suiteName = suiteName1;
            this.orcFile = orcFile1;
            this.expecteds = expecteds1;
            this.bindings = bindings1;
	    }

        @Override
        public void runTest() throws Throwable {
            System.out.println("\n==== Starting " + orcFile + " ====");
            try {
                final String actual = OrcForTesting.compileAndRun(orcFile.getPath(), TESTING_TIMEOUT, bindings);
                if (!expecteds.contains(actual)) {
                    throw new AssertionError("Unexpected output:\n" + actual);
                }
            } catch (CompilationException ce) {
                throw new AssertionError(ce.getMessageAndDiagnostics());
            }
        }

        @Override
        public String toString() {
          return getName() + "(" + suiteName + ")";
        }
    }

    public static TestSuite buildSuite(final String suitename, Class<? extends OrcTestCase> testCaseClass, final OrcBindings bindings, final File... examplePaths) {
        return buildSuite(suitename, testCaseClass, bindings, true, examplePaths);
    }

    public static TestSuite buildSuite(final String suitename, Class<? extends OrcTestCase> testCaseClass, final OrcBindings bindings, boolean ignoreNoExpected, final File... examplePaths) {
        final TestSuite suite = new TestSuite(suitename);
        for (final File examplePath : examplePaths) {
            final LinkedList<File> files = new LinkedList<File>();
            TestUtils.findOrcFiles(examplePath, files);
            TestSuite nestedSuite = null;
            for (final File file : files) {
                final ExpectedOutput expecteds;
                try {
                    expecteds = new ExpectedOutput(file);
                } catch (final IOException e) {
                    throw new AssertionError(e);
                }
                // skip tests with no expected output
                if (expecteds.isEmpty() && ignoreNoExpected) {
                    continue;
                }
                if (nestedSuite == null) {
                    nestedSuite = new TestSuite(examplePath.toString());
                    suite.addTest(nestedSuite);
                }
                OrcTestCase tc;
                try {
                    tc = testCaseClass.newInstance();
                } catch (InstantiationException e) {
                    // Shouldn't happen -- class is a sibling inner class of this class
                    throw new AssertionError(e);
                } catch (IllegalAccessException e) {
                    // Shouldn't happen -- class is a sibling inner class of this class
                    throw new AssertionError(e);
                }
                final String testname = file.toString().startsWith(examplePath.getPath()+File.separator) ? file.toString().substring(examplePath.getPath().length()+1) : file.toString();
                tc.otcInit(suitename, testname, file, expecteds, bindings);
                nestedSuite.addTest(tc);
            }
        }
        return suite;
    }

	public static void findOrcFiles(final File base, final List<File> files) {
		if (base.isFile() && base.getPath().endsWith(".orc")) {
			files.add(base);
			return;
		}
		final File[] list = base.listFiles();
		if (list == null) {
			return;
		}
		for (final File file : list) {
			if (file.isDirectory()) {
				findOrcFiles(file, files);
			} else if (file.getPath().endsWith(".orc")) {
				files.add(file);
			}
		}
	}
}
