//
// HaltException.scala -- Scala exception HaltException
// Project OrcScala
//
// Created by amp in 2016.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.error.runtime

/** Notify the enclosing code that a direct Orc call has halted.
  *
  * If cause is provided it is attached so the runtime can report it
  * correctly. It should only be attached when actually meaningful.
  * The default should be the zero argument constructor which does
  * not attach a cause.
  */
final class HaltException(cause: Throwable) extends RuntimeException(null, cause) {
  def this() = {
    this(null)
  }

  override def fillInStackTrace(): Throwable = {
    if (HaltException.captureHaltStackTraces) {
      super.fillInStackTrace()
    } else {
      null
    }
  }
}

object HaltException {
  @inline
  final val captureHaltStackTraces = false
}
