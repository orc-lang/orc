//
// TestUtils.java -- Java class TestUtils
// Project OrcTests
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.util;

import static org.junit.Assume.assumeNoException;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import orc.error.compiletime.CompilationException;
import orc.error.compiletime.FeatureNotSupportedException;
import orc.script.OrcBindings;
import orc.test.item.OrcForTesting;

import junit.framework.TestCase;
import junit.framework.TestSuite;

public final class TestUtils {

    /**
     * If the Java system property orc.executionlog.dir has ${testRunNumber} in
     * it, replace that with the TestRunNumber. Also, create the directory,
     * including and needed parent directories.
     */
    static {
        initSystem();
    }

    private TestUtils() {
        /* Only static members */
    }

    public static void initSystem() {
        final String execLogDir = System.getProperty("orc.executionlog.dir");
        if (execLogDir != null && execLogDir.contains("${testRunNumber}")) {
            final String newExecLogDir = Pattern.compile("${testRunNumber}", Pattern.LITERAL).matcher(execLogDir).replaceAll(TestRunNumber.singletonNumber());
            System.setProperty("orc.executionlog.dir", newExecLogDir);
        }
        final String execLogDir2 = System.getProperty("orc.executionlog.dir");
        if (execLogDir2 != null && !execLogDir2.isEmpty()) {
            new File(execLogDir2).mkdirs();
        }
    }

    @FunctionalInterface
    public interface OrcTestCaseFactory {
        OrcTestCase apply(final String suiteName, final String testName, final File orcFile, final ExpectedOutput expecteds, final OrcBindings bindings);
    }

    abstract public static class OrcTestCase extends TestCase {
        /**
         * The timeout to use for testing. This provides a central location to
         * change it.
         */
        public static final long TESTING_TIMEOUT = 15L;

        protected String suiteName;
        protected File orcFile;
        protected ExpectedOutput expecteds;
        protected OrcBindings bindings;

        public OrcTestCase(final String suiteName1, final String testName, final File orcFile1, final ExpectedOutput expecteds1, final OrcBindings bindings1) {
            super(testName);
            suiteName = suiteName1;
            orcFile = orcFile1;
            expecteds = expecteds1;
            bindings = bindings1;
        }

        @Override
        protected void runTest() throws Throwable {
            System.out.println("\n==== Starting " + getName() + " ====");
            try {
                final String actual = OrcForTesting.compileAndRun(orcFile.getPath(), TESTING_TIMEOUT, bindings);
                evaluateResult(actual);
            } catch (final FeatureNotSupportedException ce) {
                assumeNoException(ce);
            } catch (final CompilationException ce) {
                throw new AssertionError(ce.getMessageAndDiagnostics());
            }
        }

        /**
         * Analyze test result, and throw an AssertionError for failures.
         */
        protected void evaluateResult(final int exitStatus, final String actual) throws AssertionError {
            evaluateResult(actual);
        }

        /**
         * Analyze test result, and throw an AssertionError for failures.
         */
        protected void evaluateResult(final String actual) throws AssertionError {
            if (!expecteds.contains(actual)) {
                throw new AssertionError("Unexpected output:\n" + actual);
            }
        }

        @Override
        public String toString() {
            return getName() + "(" + suiteName + ")";
        }
    }

    public static TestSuite buildSuite(final String suitename, final OrcTestCaseFactory testCaseFactory, final OrcBindings bindings, final File... examplePaths) {

        TestEnvironmentDescription.dumpAtShutdown();

        final TestSuite suite = new TestSuite(suitename);
        for (final File examplePath : examplePaths) {
            final LinkedList<File> files = new LinkedList<>();
            TestUtils.findOrcFiles(examplePath, files);
            TestSuite nestedSuite = null;
            for (final File file : files) {
                final ExpectedOutput expecteds;
                try {
                    expecteds = new ExpectedOutput(file);
                } catch (final IOException e) {
                    throw new AssertionError(e);
                }
                if (expecteds.isEmpty()) {
                    continue;
                }
                if (nestedSuite == null) {
                    nestedSuite = new TestSuite(examplePath.toString());
                    suite.addTest(nestedSuite);
                }
                final String testname = file.toString().startsWith(examplePath.getPath() + File.separator) ? file.toString().substring(examplePath.getPath().length() + 1) : file.toString();
                final OrcTestCase tc = testCaseFactory.apply(suitename, testname, file, expecteds, bindings);
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
