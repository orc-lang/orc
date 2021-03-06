//
// experimentMetadata.scala -- Scala classes FactorDescription, FactorValue, and trait ExperimentalCondition
// Project OrcTests
//
// Created by jthywiss on Oct 9, 2017.
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.util

import java.io.{ IOException, OutputStreamWriter }
import java.nio.file.{ Files, Path, Paths }

import scala.collection.JavaConverters.{ asScalaIteratorConverter, propertiesAsScalaMapConverter }
import scala.collection.mutable.{ Buffer, StringBuilder }

import orc.util.{ CsvWriter, ExecutionLogOutputStream, WikiCreoleTableWriter }

/** Description of a factor of an experiment.
  * A factor is an independent variable, i.e. a parameter to the experiment
  * process.
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
  override def toString = toFormattedStringWithId
  def toFormattedString = name + (if (unit != null && unit.nonEmpty) s" ($unit)" else "")
  def toFormattedStringWithId = toFormattedString + " [" + id + "]"
  def toDebugString = super.toString
}

/** A value of a factor of an experiment.
  * For quantities, the value is a magnitude in the units specified in the
  * FactorDescription.
  *
  * For example, fuelFlow is 1.94 kg/s.
  *
  * @see FactorDescription
  * @author jthywiss
  */
case class FactorValue(factor: FactorDescription, value: Any) {
  override def toString = toFormattedString
  def toFormattedString = value.toString + (if (factor.unit != null && factor.unit.nonEmpty) s" ${factor.unit}" else "")
  def toDebugString = super.toString
}

object FactorValue {
  val factorPropertyPrefix = "orc.test.factor."

  val factorValuesTableColumnTitles = Seq("Factor name", "Value", "Units", "ID", "Comments")

  /** Each process in an experiment that writes experimental-condition-
    * dependent files should call this method to write a file that associates
    * the experimental condition (factor) values with its output files.
    *
    * This overload has a Scala-and-Orc-convenient signature.
    * The elements of factorValues must be either FactorValue instances or
    * 5-tuples containing Factor name, Value, Units, ID, and Comments.
    *
    * @see FactorValue
    */
  @throws[IOException]
  def writeFactorValuesTable(factorValues: Traversable[Product]): Unit = {
    writeFactorValuesTable(_.writeRowsOfProducts(factorValues.map(_ match {
      case fv: FactorValue => ((fv.factor.name, fv.value, fv.factor.unit, fv.factor.id, fv.factor.comments))
      case t: Product if t.productArity == 5 => t
      case _ => throw new IllegalArgumentException("writeFactorValuesTable: factorValues must be either FactorValue instances or 5-tuples")
    })))
  }

  /** Write a set of factors plus factors from system properties.
    *
    * @see FactorValue.writeFactorValuesTable
    */
  @throws[IOException]
  def writeFactorValuesTableWithPropertyFactors(factorValues: Traversable[Product]): Unit = {
    val properties = System.getProperties.asScala
    val propertyFactors = properties.filterKeys(s => s.startsWith(factorPropertyPrefix) && s.indexOf(".", factorPropertyPrefix.size) == -1)
      .map {
        case (k, v) =>
          val id = k.substring(factorPropertyPrefix.size)
          val name = System.getProperty(s"$k.name", id)
          val comments = System.getProperty(s"$k.comments", "")
          val unit = System.getProperty(s"$k.unit", "")
          (name, v, unit, id, comments)
      }

    writeFactorValuesTable(propertyFactors ++ factorValues)
  }

  /** Each process in an experiment that writes experimental-condition-
    * dependent files should call this method to write a file that associates
    * the experimental condition (factor) values with its output files.
    *
    * This overload has a Java-convenient signature.
    * The inner arrays must contain 5 elements: Factor name, Value, Units,
    * ID, and Comments.
    */
  @throws[IOException]
  def writeFactorValuesTable(factorValues: Array[Array[AnyRef]]): Unit = {
    writeFactorValuesTable(_.writeRowsOfTraversables(factorValues.map({
      e =>
        if (e.length == 5)
          e.toTraversable
        else
          throw new IllegalArgumentException("writeFactorValuesTable: factorValues must be either FactorValue instances or 5-tuples")
    })))
  }

  @throws[IOException]
  protected def writeFactorValuesTable(writeRows: CsvWriter => Unit): Unit = {
    ExecutionLogOutputStream.apply("factor-values", "csv", "Factor values table") match {
      case Some(csvOut) => {
        try {
          val csvOsw = new OutputStreamWriter(csvOut, "UTF-8")
          try {
            val csvWriter = new CsvWriter(csvOsw)
            csvWriter.writeHeader(factorValuesTableColumnTitles)
            writeRows(csvWriter)
          } finally {
            csvOsw.close()
          }
        } finally {
          csvOut.close()
        }
      }
      case None => /* Don't write factor values table when orc.executionlog.dir not set */
    }
  }

}

/** Specifies an assignment for the experiment factors, i.e. one value
  * for each experiment parameter.
  *
  * A sequence of ExperimentalConditions is executed as an experiment.
  *
  * ExperimentalCondition is a Product, so that it can be mixed in to
  * Tuples and case class instances.
  *
  * @see FactorDescription
  * @see FactorValue
  * @author jthywiss
  */
trait ExperimentalCondition extends Product {
  def factorDescriptions: Iterable[FactorDescription]

  def factorValueIterator: Iterator[FactorValue] = productIterator.zip(factorDescriptions.iterator).map({ case (v, d) => FactorValue(d, v) })

  def toMap: Map[String, AnyRef] = Map(factorDescriptions.zipWithIndex.map({ case (fd, i) => ((fd.id, productElement(i).asInstanceOf[AnyRef])) }).toSeq: _*)

  def toJvmArgs: Iterable[String] = for ((k, v) <- toMap) yield s"-Dorc.test.$k=$v"

}

object ExperimentalCondition {

  /** Each experiment run should call this with the sequence of
    * ExperimentalConditions that will tried during this run.  This method
    * will write "experimental-conditions.csv" and ...".creole" in the run's
    * raw-output directory, recording the given factor values.  If the
    * "orc.executionlog.dir" system property is set, it will be used as the
    * name of the raw-output directory.  If it is not set, the system property
    * will be set to "runs/{yyyyMMdd-innn}/raw-output".  The directory will be
    * created, if needed.
    */
  def writeExperimentalConditionsTable(experimentalConditions: Traversable[ExperimentalCondition]): Unit = {
    System.setProperty("orc.executionlog.dir", Option(System.getProperty("orc.executionlog.dir")).getOrElse("runs/" + TestRunNumber.singletonNumber + "/raw-output"))
    Files.createDirectories(Paths.get(System.getProperty("orc.executionlog.dir")))

    val tableColumnTitles = experimentalConditions.head.factorDescriptions.map(_.toString)

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

  /** Read experimental conditions for an experiment run from a file.
    *
    * The file must be in CSV format, with a header row of factor descriptions,
    * each formatted as "Factor name (unit) [id]".
    */
  def readFrom[EC <: ExperimentalCondition](experimentalConditionsFile: Path, factors: Seq[FactorDescription], parseEcLine: Seq[String] => EC): Traversable[EC] = {
    import State._

    /* Split a CSV file record into fields. Slightly more liberal than RFC 4180, accepting more then just pure-ASCII printable chars. */
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
            } else if (ch == '\"') {
              barf(ch)
            } else {
              currField += ch
            }
          case InEscapedField =>
            if (ch == '\"') {
              currState = InEscapedFieldQuote
            } else {
              currField += ch
            }
          case InEscapedFieldQuote =>
            if (ch == '\"') {
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
    val ecBufReader = Files.newBufferedReader(experimentalConditionsFile)
    try {
      val lines = ecBufReader.lines().iterator
      if (!lines.hasNext()) {
        throw new DataFormatException(experimentalConditionsFile, "Empty file")
      }
      val headerRow = splitCsvRecord(lines.next()).mkString(",")
      val expectedHeaderRow = factors.map(_.toString).mkString(",")
      if (headerRow != expectedHeaderRow) {
        throw new DataFormatException(experimentalConditionsFile, s"Unexpected header -- Expected $expectedHeaderRow, got $headerRow")
      }
      val experimentalConditions = lines.asScala.map(line => parseEcLine(splitCsvRecord(line)))
      experimentalConditions.toList
    } finally {
      ecBufReader.close()
    }
  }

  private sealed abstract class State
  private object State {
    final case object StartOfField extends State
    final case object InNonEscapedField extends State
    final case object InEscapedField extends State
    final case object InEscapedFieldQuote extends State
  }

  class DataFormatException(message: String) extends Exception(message) {
    def this(file: Path, message: String) = this(file.toString + ": " + message)
  }

}
