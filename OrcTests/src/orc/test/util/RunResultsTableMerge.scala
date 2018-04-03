//
// RunResultsTableMerge.scala -- Scala object RunResultsTableMerge
// Project OrcTests
//
// Created by jthywiss on Oct 8, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.util

import java.io.{ FileNotFoundException, IOException }
import java.nio.file.{ Files, Path, Paths }

import scala.collection.JavaConverters.asScalaIteratorConverter

/** A utility to merge multiple tables (CSV files) that represent various
  * experimental conditions into a single table that with the entire run's
  * experimental conditions and corresponding results.  For example, if
  * a run wrote the files A_times.csv, B_times.csv, and C_times.csv, this
  * would merge those with the {A|B|C}_factor-values.csv data into a table
  * (CSV format) written to stdout.
  *
  * This requires a experimental-conditions.csv file to determine factor
  * names, and *factor-values.csv for each *<file-base-name>.csv file.
  *
  * @see ExperimentalCondition
  * @author jthywiss
  */
object RunResultsTableMerge {
  /* Exit status codes. Values >= 64 are from BSD conventions in sysexit.h */
  val EXIT_USAGE = 64 // command line usage error
  val EXIT_DATAERR = 65 // data format error
  val EXIT_NOINPUT = 66 // cannot open input
  val EXIT_IOERR = 74 // input/output error

  def main(args: Array[String]): Unit = {
    if (args.length != 2) {
      System.err.println(s"""Usage: ${getClass.getSimpleName.stripSuffix("$")} run-directory file-base-name
                            |    where run-directory is the parent of the raw-output directory, and
                            |          file-base-name is the base name of the CSV file with the result tables.""".stripMargin)
      System.exit(EXIT_USAGE)
    }

    try {
      mergeResultsTables(args(0), args(1))
    } catch {
      case dfe: DataFormatException => { Console.err.println(dfe.getMessage); System.exit(EXIT_DATAERR) }
      case fnf: FileNotFoundException => { Console.err.println("File not found: " + fnf.getMessage); System.exit(EXIT_NOINPUT) }
      case ioe: IOException => { Thread.currentThread.getUncaughtExceptionHandler.uncaughtException(Thread.currentThread(), ioe); System.exit(EXIT_IOERR) }
    }
  }

  class DataFormatException(message: String) extends Exception(message) {}

  @throws[DataFormatException]
  @throws[IOException]
  def mergeResultsTables(runDirectory: String, fileBaseName: String) = {
    val rootPath = Paths.get(runDirectory, "raw-output")
    //System.err.println(s"Analyzing run ${rootPath.toAbsolutePath()}\n")

    val experimentalConditionFactorNames = readExperimentalConditionFactorNames(rootPath)

    val extraFactors = readAllFactorNames(rootPath).filterNot(experimentalConditionFactorNames.contains(_))

    val factorNamesInOrder = extraFactors ++ experimentalConditionFactorNames

    val headerPrefix = factorNamesInOrder.mkString(",") + ","

    //TODO: If the same factor ID is used with different names, fail or warn-and-merge
    //TODO: Maybe: Add unit consistency checks 

    var firstHeader: String = null
    for (path <- Files.find(rootPath, 1, (p, a) => p.toString.endsWith(fileBaseName + ".csv") || p.toString.endsWith(fileBaseName + "_0.csv")).iterator.asScala) {
      val reader = Files.newBufferedReader(path)
      val lines = reader.lines().iterator
      if (!lines.hasNext()) {
        Console.err.println(s"WARNING: Empty file: ${path.toAbsolutePath}")
      } else {
        val header = lines.next()
        if (firstHeader == null) {
          firstHeader = header
          print(headerPrefix + header + "\r\n")
        } else {
          if (header != firstHeader) {
            throw new DataFormatException(s"Inconsistent headers: $firstHeader vs. $header: ${path.toAbsolutePath}")
          }
        }
        val factorValuesFile = Paths.get(path.toString.replace(fileBaseName, "factor-values"))
        val factorValues = readFactorValues(factorValuesFile)
        val rowPrefix = factorNamesInOrder.map(factorValues.getOrElse(_, "")).mkString(",") + ","
        for (line <- lines.asScala) {
          print(rowPrefix + line + "\r\n")
        }
        reader.close()
      }
    }
  }

  @throws[DataFormatException]
  @throws[IOException]
  def readExperimentalConditionFactorNames(rootPath: Path) = {
    val factorNames = scala.collection.mutable.Buffer.empty[String]
    val experimentalConditionsFile = rootPath.resolve("experimental-conditions.csv")
    val reader = Files.newBufferedReader(experimentalConditionsFile)
    val lines = reader.lines().iterator
    if (!lines.hasNext()) {
      throw new DataFormatException(s"Empty file: ${experimentalConditionsFile.toAbsolutePath}")
    }
    for (factorName <- lines.next().split(",")) {
      if (!factorNames.contains(factorName)) {
        factorNames.append(factorName)
      }
    }
    reader.close()
    factorNames
  }

  @throws[DataFormatException]
  @throws[IOException]
  def readAllFactorNames(rootPath: Path) = {
    val factorNames = scala.collection.mutable.Buffer.empty[String]
    for (path <- Files.find(rootPath, 99999, (p, a) => p.toString.endsWith("factor-values.csv") || p.toString.endsWith("factor-values_0.csv")).iterator.asScala) {
      val reader = Files.newBufferedReader(path)
      val lines = reader.lines().iterator
      if (!lines.hasNext() || lines.next() != "Factor name,Value,Units,ID,Comments") {
        throw new DataFormatException(s"Empty file or file with unexpected header: ${path.toAbsolutePath}")
      }
      for (line <- lines.asScala) {
        val fields = line.split(",", -1)
        val factorName = fields(0) + (if (fields(2).nonEmpty) " (" + fields(2) + ")" else "") + (if (fields(3).nonEmpty) " [" + fields(3) + "]" else "")
        if (!factorNames.contains(factorName)) {
          factorNames.append(factorName)
        }
      }
      reader.close()
    }
    factorNames
  }

  @throws[DataFormatException]
  @throws[IOException]
  def readFactorValues(factorValuesFile: Path) = {
    val factorNamesAndValues = scala.collection.mutable.Map.empty[String, Any]
    val reader = Files.newBufferedReader(factorValuesFile)
    val lines = reader.lines().iterator
    if (!lines.hasNext() || lines.next() != "Factor name,Value,Units,ID,Comments") {
      throw new DataFormatException(s"Empty file or file with unexpected header: ${factorValuesFile.toAbsolutePath}")
    }
    for (line <- lines.asScala) {
      val fields = line.split(",", -1)
      val factorName = fields(0) + (if (fields(2).nonEmpty) " (" + fields(2) + ")" else "") + (if (fields(3).nonEmpty) " [" + fields(3) + "]" else "")
      factorNamesAndValues += ((factorName, fields(1)))
    }
    reader.close()
    factorNamesAndValues
  }

}
