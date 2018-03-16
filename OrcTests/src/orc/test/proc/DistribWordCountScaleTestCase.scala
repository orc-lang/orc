//
// DistribWordCountScaleTestCase.scala -- Scala class DistribWordCountScaleTestCase
// Project OrcTests
//
// Created by jthywiss on Oct 6, 2017.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.proc

import java.io.File

import orc.script.OrcBindings
import orc.test.item.distrib.WordCount
import orc.test.util.{ ExpectedOutput, ExperimentalCondition, FactorDescription }

import junit.framework.{ Test, TestSuite }

/** JUnit test case for a distributed-Orc scaling test.
  *
  * @author jthywiss
  */
class DistribWordCountScaleTestCase(
    val factorValues: DistribWordCountScaleTestCase.DistribScaleExperimentalCondition,
    suiteName: String,
    testName: String,
    orcFile: File,
    expecteds: ExpectedOutput,
    bindings: OrcBindings,
    testContext: Map[String, AnyRef],
    leaderSpecs: DistribTestCase.DOrcRuntimePlacement,
    followerSpecs: Seq[DistribTestCase.DOrcRuntimePlacement])
  extends DistribTestCase(suiteName, testName, orcFile, expecteds, bindings, testContext, leaderSpecs, followerSpecs) {

  override def outFilenamePrefix: String = super.outFilenamePrefix + "_" + factorValues.productIterator.mkString("_")

  @throws[AssertionError]
  override protected def evaluateResult(exitStatus: Int, actual: String): Unit = {
    /* Weak failure checking, just look at leader's exit value */
    if (exitStatus != 0) {
      throw new AssertionError(s"${getName} failed: exitStatus=$exitStatus")
    }
  }

  @throws[AssertionError]
  override protected def evaluateResult(actual: String): Unit = {
    /* No exit value, so just always succeed */
  }
}

object DistribWordCountScaleTestCase {

  def buildTestSuite(): Test = {
    val experimentalConditions = ExperimentalCondition.readFrom(new File("test_data/performance/distrib/wordcount/experimental-conditions.csv"), factors, DistribScaleExperimentalCondition.parse(_))
    ExperimentalCondition.writeExperimentalConditionsTable(experimentalConditions)
    DistribTestCase.setUpTestSuite()
    val programPaths = Array(new File("test_data/performance/distrib/wordcount/"))
    val testRunSuite = new TestSuite("DistribWordCountScaleTest")
    for (experimentalCondition <- experimentalConditions) {

      val testContext = experimentalCondition.toMap

      val suiteForOneCondition = DistribTestCase.buildSuite(
        (s, t, f, e, b, tc, ls, fs) => new DistribWordCountScaleTestCase(experimentalCondition, s, t, f, e, b, tc, ls, fs),
        testContext,
        programPaths)
      if (experimentalCondition.dOrcNumRuntimes == 1) {
        /* Special case: Measure the Java WordCount in the 1 runtime (non-distributed) experimental condition */
        suiteForOneCondition.addTest(new RunMainMethodTestCase(s"WordCount_${experimentalCondition.productIterator.mkString("_")}", testContext, classOf[WordCount]))
      }
      suiteForOneCondition.setName(experimentalCondition.toString)
      testRunSuite.addTest(suiteForOneCondition)
    }
    testRunSuite
  }

  val factors = Seq(
    FactorDescription("numInputFiles", "Number of files read", "", ""),
    //FactorDescription("repeatRead", "Reads per file", "", ""),
    FactorDescription("dOrcNumRuntimes", "Cluster size", "", ""))
    // Add numRepetitions?

  case class DistribScaleExperimentalCondition(numInputFiles: Int, /*repeatRead: Int,*/ dOrcNumRuntimes: Int) extends ExperimentalCondition {
    override def factorDescriptions = factors
    override def toString = s"(numInputFiles=$numInputFiles, dOrcNumRuntimes=$dOrcNumRuntimes)"
  }

  object DistribScaleExperimentalCondition {
    def parse(fields: Seq[String]) = {
      assert(fields.size == 2, "Expecting two argument fields to DistribScaleExperimentalCondition.parse")
      DistribScaleExperimentalCondition(fields(0).toInt, fields(1).toInt)
    }
  }

}
