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

/** A registry for tracing or other data collection utilities which allows them to be triggered to dump their output.
 * 
 */
object DumperRegistry {
  private var operations: Seq[(String) => Unit] = Seq()
  
  def register(operation: (String) => Unit): Unit = synchronized {
    operations +:= operation
  }
  
  def dump(name: String): Unit = synchronized {
    operations foreach { _(name) }
  }
}