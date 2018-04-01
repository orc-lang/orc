//
// DumperRegistry.scala -- Scala object DumperRegistry
// Project OrcScala
//
// Created by amp on Jan, 2018.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.util

import java.io.OutputStreamWriter

/** A registry for tracing or other data collection utilities which allows them to be triggered to dump their output.
 * 
 */
object DumperRegistry {
  private var operations: Seq[(String) => Unit] = Seq()
  private var clears: Seq[() => Unit] = Seq()
  
  def register(operation: (String) => Unit): Unit = synchronized {
    operations +:= operation
  }

  def registerClear(operation: () => Unit): Unit = synchronized {
    clears +:= operation
  }

  def registerCSVLineDumper(basename: String, extension: String, description: String, tableColumnTitles: Seq[String])(operation: (String) => Product): Unit = {
    ExecutionLogOutputStream.createOutputDirectoryIfNeeded()
    val csvOut = ExecutionLogOutputStream(basename, extension, description)
    if (csvOut.isDefined) {
      val traceCsv = new OutputStreamWriter(csvOut.get, "UTF-8")
      val csvWriter = new CsvWriter(traceCsv.append(_))  
      csvWriter.writeHeader(tableColumnTitles)
  
      register { name => 
        csvWriter.writeRow(operation(name))
        traceCsv.flush()
      }
    }
  }
  
  def dump(name: String): Unit = synchronized {
    operations foreach { _(name) }
  }
  def clear(): Unit = synchronized {
    clears foreach { _() }
  }
}