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

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import scala.collection.JavaConverters;

import orc.test.item.distrib.WordCount;
import orc.test.util.OsCommand;
import orc.test.util.OsCommandResult;
import orc.test.util.TestRunNumber;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit test suite for distributed-Orc tests.
 *
 * @author jthywiss
 */
public class DistribScaleTest {

    /*
     * This TestSuite factory is in Java to keep JUnit from being confused by
     * Scala.
     */
    public static Test suite() {
        final File[] programPaths = { new File("test_data/performance/distrib") };
        final TestSuite suite = DistribTestCase.buildSuite(DistribScaleTestCase.class, programPaths);
        suite.addTest(new RunMainMethodTest(WordCount.class));
        return suite;
    }

    public static class RunMainMethodTest extends TestCase {
        protected Class<?> testItem;
        protected String[] mainArgs;

        public RunMainMethodTest(final Class<?> clazz, final String... args) {
            setName(clazz.getSimpleName());
            testItem = clazz;
            mainArgs = args;
        }

        @Override
        protected void runTest() throws Throwable {
            final String outFilenamePrefix = testItem.getSimpleName();
            final String runOutputDir = "runs/" + TestRunNumber.singletonNumber() + "/raw-output";
            final File testOutFile = new File(runOutputDir, outFilenamePrefix + ".out");
            final File testErrFile = new File(runOutputDir, outFilenamePrefix + ".err");

            final String orcVersion = orc.Main.versionProperties().getProperty("orc.version");
            final String javaCmd = "java"; //DistribTestConfig.expanded$.MODULE$.apply("javaCmd");
            final String dOrcClassPath = "../OrcScala/build/orc-" + orcVersion + ".jar:../OrcScala/lib/*:../PorcE/build/classes:../OrcTests/build"; //DistribTestConfig.expanded$.MODULE$.getIterableFor("dOrcClassPath").get().mkString(File.pathSeparator);
            final List<String> javaRunCommand = Arrays.asList(new String[] {
                    javaCmd,
                    "-cp",
                    dOrcClassPath,
                    "-Djava.util.logging.config.file=config/logging.properties",
                    "-Dsun.io.serialization.extendedDebugInfo=true",
                    "-Dorc.config.dirs=config",
                    "-Dorc.executionlog.dir=" + runOutputDir,
                    "-Dorc.executionlog.fileprefix=" + outFilenamePrefix + "_",
                    "-Dorc.executionlog.filesuffix=_0", testItem.getName()
            }); //DistribTestConfig.expanded$.MODULE$.getIterableFor("jvmOpts").get().toSeq();
            javaRunCommand.addAll(Arrays.asList(mainArgs));

            final scala.collection.Seq<String> javaRunCommandSeq = JavaConverters.collectionAsScalaIterable(javaRunCommand).toSeq();
            final scala.collection.Traversable<OutputStream> stdoutTee = JavaConverters.collectionAsScalaIterable(Arrays.asList(new OutputStream[] { System.out, new FileOutputStream(testOutFile) }));
            final scala.collection.Traversable<OutputStream> stderrTee = JavaConverters.collectionAsScalaIterable(Arrays.asList(new OutputStream[] { System.err, new FileOutputStream(testErrFile) }));

            System.out.println("\n==== Starting " + getName() + " ====");
            final OsCommandResult result = OsCommand.getResultFrom(javaRunCommandSeq, new File("."), "", StandardCharsets.UTF_8, true, stdoutTee, stderrTee);
            if (result.exitStatus() != 0) {
                throw new AssertionError(getName() + " failed: exitStaus=" + result.exitStatus());
            }

            System.out.println();
        }
    }

    public static class DistribScaleTestCase extends DistribTestCase {

        @Override
        protected void evaluateResult(final int exitStatus, final String actual) throws AssertionError {
            /* Weak failure checking, just look at leader's exit value */
            if (exitStatus != 0) {
                throw new AssertionError(getName() + " failed: exitStatus=" + exitStatus);
            }
        }

        @Override
        protected void evaluateResult(final String actual) throws AssertionError {
            /* No exit value, so we'll always succeed */
        }
    }
}
