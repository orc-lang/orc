//
// SimpleWorkStealingSchedulerWrapper.scala -- Scala wrapper class SimpleWorkStealingSchedulerWrapper
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce

import orc.run.extensions.{ SimpleWorkStealingScheduler }
import com.oracle.truffle.api.CompilerDirectives.{ TruffleBoundary, CompilationFinal }

object SimpleWorkStealingSchedulerWrapper {
  @CompilationFinal
  final val traceTasks = SimpleWorkStealingScheduler.traceTasks
  @CompilationFinal
  final val SchedulerExecution = SimpleWorkStealingScheduler.SchedulerExecution
  @CompilationFinal
  final val StackExecution = SimpleWorkStealingScheduler.StackExecution
  @CompilationFinal
  final val InlineExecution = SimpleWorkStealingScheduler.InlineExecution

  def shareSchedulableID(d: AnyRef, s: AnyRef): Unit = {
    if (traceTasks) {
      Boundaries.shareSchedulableID(d, s)
    }
  }

  def getSchedulableID(s: AnyRef) = {
    if (traceTasks) {
      Boundaries.getSchedulableID(s)
    } else {
      0
    }
  }

  def traceTaskParent(parent: AnyRef, child: AnyRef): Unit = {
    if (traceTasks) {
      Boundaries.traceTaskParent(parent, child)
    }
  }

  /*
  def traceTaskParent(parent: AnyRef, child: Long): Unit = {
    if (traceTasks) {
      Boundaries.traceTaskParent(parent, child)
    }
  }*/

  def traceTaskParent(parent: Long, child: Long): Unit = {
    if (traceTasks) {
      Boundaries.traceTaskParent(parent, child)
    }
  }

  def enterSchedulable(s: AnyRef, t: SimpleWorkStealingScheduler.SchedulableExecutionType): Unit = {
    if (traceTasks) {
      Boundaries.enterSchedulable(s, t)
    }
  }

  def enterSchedulableInline(): Long = {
    if (traceTasks) {
      Boundaries.enterSchedulableInline()
    } else 0
  }

  def currentSchedulable: AnyRef = {
    if (traceTasks) {
      Boundaries.currentSchedulable
    } else {
      null
    }
  }

  def exitSchedulable(s: AnyRef): Unit = {
    if (traceTasks) {
      Boundaries.exitSchedulable(s)
    }
  }

  def exitSchedulable(s: AnyRef, old: AnyRef): Unit = {
    if (traceTasks) {
      Boundaries.exitSchedulable(s, old)
    }
  }

  def exitSchedulable(s: Long, old: AnyRef): Unit = {
    if (traceTasks) {
      Boundaries.exitSchedulable(s, old)
    }
  }

  object Boundaries {
    @TruffleBoundary @noinline
    def shareSchedulableID(d: AnyRef, s: AnyRef): Unit = {
      SimpleWorkStealingScheduler.shareSchedulableID(d, s)
    }

    @TruffleBoundary @noinline
    def getSchedulableID(s: AnyRef) = {
      SimpleWorkStealingScheduler.getSchedulableID(s)
    }

    @TruffleBoundary @noinline
    def traceTaskParent(parent: AnyRef, child: AnyRef): Unit = {
      SimpleWorkStealingScheduler.traceTaskParent(parent, child)
    }

    @TruffleBoundary @noinline
    def traceTaskParent(parent: AnyRef, child: Long): Unit = {
      SimpleWorkStealingScheduler.traceTaskParent(parent, child)
    }

    @TruffleBoundary @noinline
    def traceTaskParent(parent: Long, child: Long): Unit = {
      SimpleWorkStealingScheduler.traceTaskParent(parent, child)
    }

    @TruffleBoundary @noinline
    def enterSchedulable(s: AnyRef, t: SimpleWorkStealingScheduler.SchedulableExecutionType): Unit = {
      SimpleWorkStealingScheduler.enterSchedulable(s, t)
    }

    @TruffleBoundary @noinline
    def enterSchedulableInline(): Long = {
      val s = SimpleWorkStealingScheduler.newSchedulableID()
      SimpleWorkStealingScheduler.traceTaskParent(SimpleWorkStealingScheduler.currentSchedulable, s)
      SimpleWorkStealingScheduler.enterSchedulable(s, InlineExecution)
      s
    }

    @TruffleBoundary @noinline
    def currentSchedulable: AnyRef = {
      SimpleWorkStealingScheduler.currentSchedulable
    }

    @TruffleBoundary @noinline
    def exitSchedulable(s: AnyRef): Unit = {
      SimpleWorkStealingScheduler.exitSchedulable(s)
    }

    @TruffleBoundary @noinline
    def exitSchedulable(s: AnyRef, old: AnyRef): Unit = {
      SimpleWorkStealingScheduler.exitSchedulable(s, old)
    }
    
    @TruffleBoundary @noinline
    def exitSchedulable(s: Long, old: AnyRef): Unit = {
      SimpleWorkStealingScheduler.exitSchedulable(s, old)
    }
  }
}
