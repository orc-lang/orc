//
// experimentMetadata.scala -- Scala classes FactorDescription, FactorValue, and trait ExperimentalCondition
// Project OrcTest
//
// Created by jthywiss on Oct 9, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.util

import java.io.{ File, OutputStreamWriter }

import orc.util.{ CsvWriter, ExecutionLogOutputStream, WikiCreoleTableWriter }
import java.io.IOException

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
  override def toString = toFormattedString
  def toFormattedString = name + (if (unit != null && unit.nonEmpty) s" ($unit)" else "")
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

  val factorValuesTableColumnTitles = Seq("Factor name", "Value", "Units", "Comments")

  /** Each process in an experiment that writes experimental-condition-
    * dependent files should call this method to write a file that associates
    * the experimental condition (factor) values with its output files.
    *
    * This overload has a Scala-and-Orc-convenient signature.
    * The elements of factorValues must be either FactorValue instances or
    * 4-tuples containing Factor name, Value, Units, and Comments.
    *
    * @see FactorValue
    */
  @throws[IOException]
  def writeFactorValuesTable(factorValues: Traversable[Product]): Unit = {
    writeFactorValuesTable(_.writeRowsOfProducts(factorValues.map(_ match {
      case fv: FactorValue => ((fv.factor.name, fv.value, fv.factor.unit, fv.factor.comments))
      case t: Product if t.productArity == 4 => t
    })))
  }

  /** Each process in an experiment that writes experimental-condition-
    * dependent files should call this method to write a file that associates
    * the experimental condition (factor) values with its output files.
    *
    * This overload has a Java-convenient signature.
    * The inner arrays must contain 4 elements: Factor name, Value, Units,
    * and Comments.
    */
  @throws[IOException]
  def writeFactorValuesTable(factorValues: Array[Array[AnyRef]]): Unit = {
    writeFactorValuesTable(_.writeRowsOfTraversables(factorValues.map(_.toTraversable)))
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
    System.setProperty("orc.executionlog.dir", System.getProperty("orc.executionlog.dir", "runs/" + TestRunNumber.singletonNumber + "/raw-output"))
    new File(System.getProperty("orc.executionlog.dir")).mkdirs()

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

}
