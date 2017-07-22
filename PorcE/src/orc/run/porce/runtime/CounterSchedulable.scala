//
// CounterSchedulable.scala -- Scala classes integrating Orc interpeter scheduler work with the ToJava scheduler.
// Project OrcScala
//
// Created by amp on Dec 6, 2014.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.runtime

import orc.Schedulable
import orc.error.runtime.HaltException
import orc.run.porce.Logger

/** A Schedulable which manages Context spawn/halt counting automatically.
  *
  * @author amp
  */
abstract class CounterSchedulable(c: Counter) extends Schedulable {
  // TODO: We know we are non-blocking because all PorcE code is non-blocking. If we can split out site calls we could set this true.
  //       A better approach would be to set this to true and then dynamically call potentiallyBlocking when we do something that could block.
  override val nonblocking = false

  /** When we are scheduled prepare for spawning.
    */
  override def onSchedule() = {
  }

  /** When execution completes halt.
    */
  override def onComplete() = {
  }
}

/** A subclass of ContextSchedulable that takes a run implementation as a Scala function.
  *
  * This is for Scala side use.
  *
  * @author amp
  */
final class CounterSchedulableFunc(c: Counter, f: () => Unit) extends CounterSchedulable(c) {
  /** Call the provided implementation and ignore KilledException.
    *
    * The exception is ignored because if something is killed it is fine to
    * just die all the way to the scheduler, but it should not effect the
    * scheduler thread.
    */
  def run(): Unit = {
    // Catch kills and continue.
    try {
      f()
    } catch {
      case _: KilledException =>
        Logger.warning(s"Caught KilledException from $f")
      case _: HaltException =>
        Logger.warning(s"Caught HaltException from $f")
    }
  }
}

