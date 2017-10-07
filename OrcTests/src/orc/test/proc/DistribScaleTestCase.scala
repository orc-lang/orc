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

import java.io.{ File, OutputStreamWriter }

import orc.script.OrcBindings
import orc.test.item.distrib.WordCount
import orc.test.util.{ ExpectedOutput, TestRunNumber }
import orc.util.{ CsvWriter, ExecutionLogOutputStream, WikiCreoleTableWriter }

import junit.framework.{ Test, TestSuite }

/** JUnit test case for a distributed-Orc scaling test.
  *
  * @author jthywiss
  */
class DistribScaleTestCase(
    val factorValues: DistribScaleTestCase.Expt,
    suiteName: String,
    testName: String,
    orcFile: File,
    expecteds: ExpectedOutput,
    bindings: OrcBindings,
    testContext: Map[String, AnyRef],
    leaderSpecs: DistribTestCase.DOrcRuntimePlacement,
    followerSpecs: Seq[DistribTestCase.DOrcRuntimePlacement])
  extends DistribTestCase(suiteName, testName, orcFile, expecteds, bindings, testContext, leaderSpecs, followerSpecs) {

  override def outFilenamePrefix: String = s"${super.outFilenamePrefix}_${factorValues.repeatRead}_${factorValues.dOrcNumRuntimes}"

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
    writeExperimentalConditionsTable(experimentalConditions)
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

  def readExperimentalConditions(): Iterable[Expt] = {
    //FIXME: Read from file
    Seq(
      //  | Reads per file | Cluster size |
      Expt(3, 3),
      Expt(6, 3),
      Expt(12, 3),
      Expt(3, 6),
      Expt(6, 6),
      Expt(12, 6),
      Expt(3, 12),
      Expt(6, 12),
      Expt(12, 12))
  }

  def writeExperimentalConditionsTable(experimentalConditions: Traversable[Expt]): Unit = {
    System.setProperty("orc.executionlog.dir", System.getProperty("orc.executionlog.dir", "runs/" + TestRunNumber.singletonNumber + "/raw-output"))
    new File(System.getProperty("orc.executionlog.dir")).mkdirs()

    val tableColumnTitles = factors.map(_.toString)

    val csvOut = ExecutionLogOutputStream.apply("experimental-conditions", "csv", "Experimental conditions table (list of factor values tried)").get
    val csvOsw = new OutputStreamWriter(csvOut, "UTF-8")
    val csvWriter = new CsvWriter(csvOsw)
    csvWriter.writeHeader(tableColumnTitles)
    csvWriter.writeRows(experimentalConditions)
    csvOsw.close()
    csvOut.close()

    val creoleOut = ExecutionLogOutputStream.apply("experimental-conditions", "creole", "Experimental conditions table (list of factor values tried)").get
    val creoleOsw = new OutputStreamWriter(creoleOut, "UTF-8")
    val creoleWriter = new WikiCreoleTableWriter(creoleOsw)
    creoleWriter.writeHeader(tableColumnTitles)
    creoleWriter.writeRows(experimentalConditions)
    creoleOsw.close()
    creoleOut.close()
  }

  /** Description of a factor of an experiment.
    *
    * id is a identifier, for example "fuelFlow".
    * Syntax: a letter, followed by letters, numbers, and underscores.
    * Convention: lowerCamelCase.
    *
    * name is a human-readable name, for example "Fuel flow".
    * Convention: Sentence capitalization.
    *
    * unit is the unit symbol for the values, for example "kg/s".
    * Common units: second "s", bit "bit", byte "B", Hertz "Hz".
    * Note the prefixes (k, M, G, etc.) are decimal.
    * Prefixes for binary multiples have an "i" (Ki, Mi, Gi, etc.).
    * I.e., 1 MB = 1000000 B, but 1 MiB = 1048576 B.
    * Counts of events or entities are considered dimensionless, and have no unit symbol.
    *
    * @author jthywiss
    */
  case class FactorDescription(id: String, name: String, unit: String, comments: String) {
    override def toString = name + (if (unit != null && unit.nonEmpty) s" ($unit)" else "")
  }

  val factors = Seq(
    FactorDescription("repeatRead", "Reads per file", "", ""),
    FactorDescription("dOrcNumRuntimes", "Cluster size", "", ""))
  // Add numRepetitions?

  trait ExperimentalCondition extends Product {
    def factorDescriptions: Iterable[FactorDescription]

    def toMap: Map[String, AnyRef] = Map(factorDescriptions.zipWithIndex.map({ case (fd, i) => ((fd.id, productElement(i).asInstanceOf[AnyRef])) }).toSeq: _*)

  }

  case class Expt(repeatRead: Int, dOrcNumRuntimes: Int) extends ExperimentalCondition {
    override def factorDescriptions = factors
    override def toString = s"(repeatRead=${repeatRead}, dOrcNumRuntimes=${dOrcNumRuntimes})"
  }

}
