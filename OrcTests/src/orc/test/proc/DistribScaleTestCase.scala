//
// DistribScaleTestCase.scala -- Scala class DistribScaleTestCase
// Project project_name
//
// Created by jthywiss on Oct 6, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
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
class DistribScaleTestCase(
    val factorValues: DistribScaleTestCase.DistribScaleExperimentalCondition,
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

  @throws(classOf[AssertionError])
  override protected def evaluateResult(exitStatus: Int, actual: String): Unit = {
    /* Weak failure checking, just look at leader's exit value */
    if (exitStatus != 0) {
      throw new AssertionError(s"${getName} failed: exitStatus=$exitStatus")
    }
  }

  @throws(classOf[AssertionError])
  override protected def evaluateResult(actual: String): Unit = {
    /* No exit value, so just always succeed */
  }
}

object DistribScaleTestCase {

  def buildTestSuite(): Test = {
    val experimentalConditions = readExperimentalConditions()
    ExperimentalCondition.writeExperimentalConditionsTable(experimentalConditions)
    DistribTestCase.setUpTestSuite()
    val programPaths = Array(new File("test_data/performance/distrib"))
    val testRunSuite = new TestSuite("DistribScaleTest")
    for (experimentalCondition <- experimentalConditions) {

      val testContext = experimentalCondition.toMap

      val suiteForOneCondition = DistribTestCase.buildSuite(
        (s, t, f, e, b, tc, ls, fs) => new DistribScaleTestCase(experimentalCondition, s, t, f, e, b, tc, ls, fs),
        testContext,
        programPaths)
      suiteForOneCondition.addTest(new RunMainMethodTestCase(s"WordCount_${experimentalCondition.repeatRead}_${experimentalCondition.dOrcNumRuntimes}", testContext, classOf[WordCount]))
      suiteForOneCondition.setName(experimentalCondition.toString)
      testRunSuite.addTest(suiteForOneCondition)
    }
    testRunSuite
  }

  val factors = Seq(
    FactorDescription("repeatRead", "Reads per file", "", ""),
    FactorDescription("dOrcNumRuntimes", "Cluster size", "", ""))
  // Add numRepetitions?

  case class DistribScaleExperimentalCondition(repeatRead: Int, dOrcNumRuntimes: Int) extends ExperimentalCondition {
    override def factorDescriptions = factors
    override def toString = s"(repeatRead=${repeatRead}, dOrcNumRuntimes=${dOrcNumRuntimes})"
  }

  def readExperimentalConditions(): Traversable[DistribScaleExperimentalCondition] = {
    //FIXME: Read from file
    Seq(
      //  | Reads per file | Cluster size |
      DistribScaleExperimentalCondition(3, 3),
      DistribScaleExperimentalCondition(6, 3),
      DistribScaleExperimentalCondition(12, 3),
      DistribScaleExperimentalCondition(3, 6),
      DistribScaleExperimentalCondition(6, 6),
      DistribScaleExperimentalCondition(12, 6),
      DistribScaleExperimentalCondition(3, 12),
      DistribScaleExperimentalCondition(6, 12),
      DistribScaleExperimentalCondition(12, 12))
  }

}
