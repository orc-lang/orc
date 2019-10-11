//
// ShutdownHook.scala -- Scala class ShutdownHook
// Project project_name
//
// Created by jthywiss on Oct 10, 2019.
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.util

import java.util.logging.Level

import scala.util.control.ControlThrowable

/** Wrapper for application JVM shutdown hooks that logs and discards
  * exceptions during hook execution.  Shutdown hooks are registered using
  * Runtime.addShutdownHook().
  *
  * @author jthywiss
  */
class ShutdownHook(target: Runnable, name: String) extends Thread(name: String) with Thread.UncaughtExceptionHandler {

  def this(name: String) = this(null, name)

  Thread.setDefaultUncaughtExceptionHandler(this)

  /** Invoked when the given thread terminates due to the given uncaught
    * exception.
    *
    * Any exception thrown by this method will be ignored by the JVM.
    */
  override def uncaughtException(t: Thread, e: Throwable): Unit = {
    e match {
      case _: ThreadDeath | _: InterruptedException | _: ControlThrowable => /* Ignore */
      case _ => {
        MainExit.printAndLogException(t, e, Level.SEVERE)
        MainExit.writeDiagnosticMessage(MainExit.progName, null, "ERROR", e.toString)
      }
    }
  }
}
