//
// DistribVertexScaleTestCase.scala -- Scala class DistribVertexScaleTestCase
// Project OrcTests
//
// Created by jthywiss on Mar 17, 2018.
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.proc

import java.nio.file.{ Path, Paths }

import scala.collection.JavaConverters.enumerationAsScalaIteratorConverter

import orc.script.OrcBindings
import orc.test.util.{ ExpectedOutput, ExperimentalCondition, FactorDescription }

import junit.framework.{ Test, TestCase, TestSuite }

/** JUnit test case for a distributed-Orc vertex scaling test.
  *
  * @author jthywiss
  */
class DistribVertexScaleTestCase(
    val factorValues: DistribVertexScaleTestCase.VertexExperimentalCondition,
    suiteName: String,
    testName: String,
    orcFile: Path,
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

object DistribVertexScaleTestCase {

  def buildTestSuite(): Test = {
    val experimentalConditions = ExperimentalCondition.readFrom(Paths.get("test_data/performance/distrib/vertex/experimental-conditions.csv"), factors, VertexExperimentalCondition.parse(_))
    ExperimentalCondition.writeExperimentalConditionsTable(experimentalConditions)
    DistribTestCase.setUpTestSuite()
    val programPaths = Array(Paths.get("test_data/performance/distrib/vertex/"))
    val testRunSuite = new TestSuite("DistribVertexScaleTest")
    for (experimentalCondition <- experimentalConditions) {

      val testContext = experimentalCondition.toMap

      val suiteForOneCondition = DistribTestCase.buildSuite(
        (s, t, f, e, b, tc, ls, fs) => new DistribVertexScaleTestCase(experimentalCondition, s, t, f, e, b, tc, ls, fs),
        testContext,
        programPaths)
      suiteForOneCondition.setName(experimentalCondition.toString)
      testRunSuite.addTest(suiteForOneCondition)
    }
    def forEachTestCase(suite: TestSuite, f: TestCase => Unit): Unit = {
      for (test <- suite.tests.asScala) {
        test match {
          case s: TestSuite => forEachTestCase(s, f)
          case c: TestCase => f(c)
        }
      }
    }
    val testNames = scala.collection.mutable.ArrayBuffer.empty[String]
    forEachTestCase(testRunSuite, { tc: TestCase => testNames += tc.getName })
    DistribTestCase.writeReadme(experimentalConditions, testNames.distinct)
    testRunSuite
  }

  val factors = Seq(
    FactorDescription("numVertices", "Number of vertices", "", ""),
    FactorDescription("probEdge", "Probability of edge", "", ""),
    FactorDescription("dOrcNumRuntimes", "Cluster size", "", ""))
    // Add numRepetitions?

  case class VertexExperimentalCondition(numVertices: Int, probEdge: Float, dOrcNumRuntimes: Int) extends ExperimentalCondition {
    override def factorDescriptions = factors
    override def toString = s"(numVertices=$numVertices, probEdge=$probEdge, dOrcNumRuntimes=$dOrcNumRuntimes)"
  }

  object VertexExperimentalCondition {
    def parse(fields: Seq[String]) = {
      assert(fields.size == 3, "Expecting three argument fields to VertexExperimentalCondition.parse")
      VertexExperimentalCondition(fields(0).toInt, fields(1).toFloat, fields(2).toInt)
    }
  }

}
