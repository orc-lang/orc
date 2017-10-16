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
import java.io.BufferedReader
import java.io.FileReader
import scala.collection.JavaConverters._
import scala.collection.mutable.StringBuilder
import scala.collection.mutable.Buffer

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
    def parse(strings: Seq[String]) = {
      assert(strings.size == 2, "Expecting two argument strings to DistribScaleExperimentalCondition.parse")
      DistribScaleExperimentalCondition(strings(0).toInt, strings(1).toInt)
    }
  }

  class DataFormatException(message: String) extends Exception(message) {
    def this(file: File, message: String) = this(file.getPath + ": " + message)
  }

  private sealed abstract class State
  private object State {
    final case object StartOfField extends State
    final case object InNonEscapedField extends State
    final case object InEscapedField extends State
    final case object InEscapedFieldQuote extends State
  }

  def readExperimentalConditions(): Traversable[DistribScaleExperimentalCondition] = {
    import State._

    /** Split a CSV file record into fields. Slightly more liberal than RFC 4180, accepting more then just pure-ASCII printable chars. */
    def splitCsvRecord(record: String): Seq[String] = {
      def barf(ch: Char) = throw new DataFormatException(s"Unexpected character -- $ch (U+${ch.toHexString})")
      val fields = Buffer[String]()
      var currState: State = StartOfField
      val currField = new StringBuilder()
      for (ch <- record) {
        currState match {
          case StartOfField =>
            currField.clear()
            if (ch == '"') {
              currState = InEscapedField
            } else if (ch == ',') {
              fields += currField.toString()
              currField.clear()
              currState = StartOfField
            } else {
              currField += ch
              currState = InNonEscapedField
            }
          case InNonEscapedField =>
            if (ch == ',') {
              fields += currField.toString()
              currField.clear()
              currState = StartOfField
            } else if (ch == '\"')  {
              barf(ch)
            } else {
              currField += ch
            }
          case InEscapedField =>
            if (ch == '\"')  {
              currState = InEscapedFieldQuote
            } else {
              currField += ch
            }
          case InEscapedFieldQuote =>
            if (ch == '\"')  {
              /* Doubled quote */
              currField += ch
              currState = InEscapedField
            } else if (ch == ',') {
              /* End of field */
              fields += currField.toString()
              currField.clear()
              currState = StartOfField
            } else {
              barf(ch)
            }
        }
      }
      /* At end of record */
      //FIXME: This actually is legal, so that end-of-lines can be in fields.
      if (currState == InEscapedField) throw new DataFormatException(s"Unexpected end of record in quoted field")
      fields += currField.toString()
      fields
    }
    val ecFile = new File("test_data/performance/distrib/experimental-conditions.csv")
    val ecReader = new FileReader(ecFile)
    try {
      val ecBufReader = new BufferedReader(ecReader)
      try {
        val lines = ecBufReader.lines().iterator
        if (!lines.hasNext()) {
          throw new DataFormatException(ecFile, "Empty file")
        }
        val headerRow = splitCsvRecord(lines.next()).mkString(",")
        val expectedHeaderRow = factors.map(_.toString).mkString(",")
        if (headerRow != expectedHeaderRow) {
          throw new DataFormatException(ecFile, s"Unexpected header -- Expected $expectedHeaderRow, got $headerRow")
        }
        val experimentalConditions = lines.asScala.map(line => DistribScaleExperimentalCondition.parse(splitCsvRecord(line)))
        experimentalConditions.toList
      } finally {
        ecBufReader.close()
      }
    } finally {
      ecReader.close()
    }
  }

}
